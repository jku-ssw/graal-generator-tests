package at.jku.ssw.java.bytecode.generator.tests;

import at.jku.ssw.java.bytecode.generator.cli.ControlValueParser;
import at.jku.ssw.java.bytecode.generator.cli.GenerationController;
import at.jku.ssw.java.bytecode.generator.generators.RandomCodeGenerator;
import at.jku.ssw.java.bytecode.generator.loaders.GeneratedClassLoader;
import at.jku.ssw.java.bytecode.generator.tests.classes.*;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.hotspot.*;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCICompiler;
import org.graalvm.collections.EconomicMap;
import org.graalvm.compiler.bytecode.Bytecodes;
import org.graalvm.compiler.hotspot.CompilationTask;
import org.graalvm.compiler.hotspot.HotSpotGraalCompiler;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import org.graalvm.compiler.options.OptionValues;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompileGeneratedClasses {

    private static final Logger logger = Logger.getLogger(CompileGeneratedClasses.class.getName());

    private static final String OUTDIR = "./generated_classes";
    private static final int REPETITIONS = 100;
    private static final int OPTIMIZATION_THRESHOLD = 10;

    private final String dir;
    private GeneratedClassLoader classLoader;
    private final HotSpotJVMCIRuntime runtime;
    private final HotSpotGraalCompiler compiler;

    @SuppressWarnings("unchecked")
    private static final List<Class<? extends Generative>> CLASSES = Arrays.asList(
            SimpleClass.class,
            ComplexClass.class,
            LotsOfMath.class,
//            Overflows.class,
//            DivisionsByZero.class,
            ManyLoops.class,
            BlockExits.class,
            ManyOverloads.class,
            HighBranchingFactor.class
    );

    CompileGeneratedClasses(String dir, HotSpotJVMCIRuntime runtime, HotSpotGraalCompiler compiler) {
        this.dir = dir;
        this.classLoader = new GeneratedClassLoader(this.dir);
        this.runtime = runtime;
        this.compiler = compiler;
    }

    private Class<?> loadClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    private void unload() {
        classLoader = new GeneratedClassLoader(dir);
        System.gc();
    }

    private static final String FAIL_PATTERN = "%s : Expected: %s -- Actual: %s";

    private static void fail(Object expected, Object actual, String message) {
        fail(FAIL_PATTERN, message, expected, actual);
    }

    private static boolean compare(Object expected, Object actual, String message) {
        if (expected == actual)
            return true;

        if (expected == null) {
            fail(null, actual, message);
            return false;
        }

        if (actual == null) {
            fail(expected, null, message);
            return false;
        }

        if (!expected.equals(actual)) {
            fail(expected, actual, message);
            return false;
        }

        return true;
    }

    private static void compare(ExecutionResult expected, ExecutionResult actual) {
        if (expected.getException() != null) {
            if (compare(expected.getException(), actual.getException(), "Expected exception")) {
                compare(expected.getException().getClass(), actual.getException().getClass(), "Wrong exception type");
                compare(expected.getException().getMessage(), actual.getException().getMessage(), "Wrong exception message");
            }
        } else {
            compare(null, actual.getException(), "Unexpected exception");

            if (expected.getResult() != null)
                compare(expected.getResult(), actual.getResult(), "Wrong result");
            else
                compare(null, actual.getResult(), "Unexpected result");

            String[] linesExpected = expected.getOutput().split(System.lineSeparator());
            String[] linesActual = actual.getOutput().split(System.lineSeparator());

            for (int i = 0; i < linesExpected.length || i < linesActual.length; i++) {
                if (i >= linesExpected.length)
                    compare("", linesActual[i], i);
                else if (i >= linesActual.length)
                    compare(linesExpected[i], "", i);
                else
                    compare(linesExpected[i], linesActual[i], i);
            }
        }
    }

    private static void compare(String expected, String actual, int line) {
        compare(expected, actual, "Line " + line);
    }

    private ExecutionResult execute(Supplier<Object> runner) throws IOException {

        final PrintStream outDefault = System.out;
        final PrintStream errDefault = System.err;

        Object result = null;
        String out;
        Throwable exception = null;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             PrintStream ps = new PrintStream(os)) {
            System.setOut(ps);
            System.setErr(ps);
            result = runner.get();
            out = os.toString();
        } catch (RuntimeException e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            out = w.toString();
            exception = e;
        }

        System.setErr(errDefault);
        System.setOut(outDefault);

        return new ExecutionResult(result, out, exception);
    }

    private String generate(Class<? extends Generative> clazz, int iter) {
        assert clazz != null;

        log(clazz, "Generating class");

        ControlValueParser parser = new ControlValueParser(Generative.args(clazz, iter));
        GenerationController controller = parser.parse();

        final String className = controller.getFileName();

        RandomCodeGenerator randomCodeGenerator = new RandomCodeGenerator(className, controller);
        randomCodeGenerator.generate();
        randomCodeGenerator.writeFile(OUTDIR);

        return className;
    }

    private ExecutionResult interpret(Class<?> clazz) throws IOException {
        log(clazz, "Interpreting code");
        try {
            return execute(
                    () -> {
                        try {
                            return clazz.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
            );
        } catch (Throwable t) {
            log(clazz, "HotSpot interpretation failed", t);
            throw t;
        }
    }

    private ExecutionResult execute(CompilationResult compiled) throws IOException {
        log(compiled.getClazz(), "Executing compiled code");
        try {
            return execute(
                    () -> {
                        try {
                            return compiled.getMain().executeVarargs((Object) new String[0]);
                        } catch (InvalidInstalledCodeException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
            );
        } catch (Throwable t) {
            log(compiled.getClazz(), "HotSpot interpretation failed", t);
            throw t;
        }
    }

    private CompilationResult compile(Class<?> clazz, boolean useProfilingInfo) throws NoSuchMethodException {
        log(clazz, "Compilation");
        try {
            // Load and initialize class
            MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();

            log(clazz, "Pre-loading constant pool classes");
            // Pre-load all classes in the constant pool.
            try {
                HotSpotResolvedObjectType objectType = (HotSpotResolvedObjectType) metaAccess.lookupJavaType(clazz);
                ConstantPool constantPool = objectType.getConstantPool();
                for (int cpi = 1; cpi < constantPool.length(); cpi++)
                    constantPool.loadReferencedType(cpi, Bytecodes.LDC);

            } catch (Throwable t) {
                // If something went wrong during pre-loading we just ignore it.
                log(clazz, "Pre-loading failed", t);
                throw t;
            }

            // retrieve the compiled code of the main method
            // assuming that it is not null
            InstalledCode main = compile(
                    clazz,
                    (HotSpotResolvedJavaMethod) metaAccess.lookupJavaMethod(
                            clazz.getMethod("main", String[].class)
                    ),
                    useProfilingInfo
            ).orElseThrow(() -> new AssertionError("Main method unexpectedly not installed"));
            List<Optional<InstalledCode>> others = Stream.concat(
                    Stream.of(
                            clazz.getDeclaredConstructors(),
                            clazz.getDeclaredMethods())
                            .flatMap(Arrays::stream)
                            .map(metaAccess::lookupJavaMethod),
                    Stream.of(
                            metaAccess.lookupJavaType(clazz).getClassInitializer()))
                    .map(HotSpotResolvedJavaMethod.class::cast)
                    .filter(Objects::nonNull)
                    .map(m -> compile(clazz, m, useProfilingInfo))
                    .collect(Collectors.toList());

            return new CompilationResult(
                    clazz,
                    main,
                    others
            );
        } catch (Throwable t) {
            log(clazz, "Error in compilation %s", t);
            throw t;
        }
    }

    private Optional<InstalledCode> compile(Class<?> clazz, HotSpotResolvedJavaMethod method, boolean useProfilingInfo) {
        log(clazz, "Compiling method %s", format(method));
        try {
            int entryBCI = JVMCICompiler.INVOCATION_ENTRY_BCI;
            HotSpotCompilationRequest request = new HotSpotCompilationRequest(method, entryBCI, 0L);
            CompilationTask task = new CompilationTask(
                    runtime,
                    compiler,
                    request,
                    useProfilingInfo,
                    true,               // install as default since code is invalidated after run anyways
                    new OptionValues(EconomicMap.create())
            );
            task.runCompilation();

            return Optional.ofNullable(task.getInstalledCode());
        } catch (Throwable t) {
            // Catch everything and print a message
            log(clazz, "Error compiling method: %s", format(method), t);
            return Optional.empty();
        }
    }

    private List<ExecutionResult> optimize(Class<?> clazz) throws IOException {
        List<ExecutionResult> r = new ArrayList<>();
        for (int i = 0; i < OPTIMIZATION_THRESHOLD; i++) {
            r.add(interpret(clazz));
        }
        return r;
    }

    private static void println(String str, Throwable t, Object... args) {
        logger.log(Level.SEVERE, String.format(str, args), t);
    }

    private static void println(String str, Object... args) {
        logger.info(String.format(str, args));
    }

    private static void fail(String str, Object... args) {
        logger.severe(String.format(str, args));
    }

    private static void log(Class<?> clazz, String str, Object... args) {
        println(clazz.getSimpleName() + " : " + str, args);
    }

    private static void log(String className, String str, Object... args) {
        println(className + " : " + str, args);
    }

    private static void log(Class<?> clazz, String str, Throwable t, Object... args) {
        println(clazz.getSimpleName() + " : " + str, t, args);
    }

    private static String format(ResolvedJavaMethod method) {
        assert method != null;
        return method.format("%H.%n(%p):%r");
    }

    /**
     * Helper that allows to check all static field value of a particular
     * class.
     *
     * @param clazz The class instance whose fields are evaluated
     * @return a map containing the field name and the corresponding value
     */
    private static Map<String, Object> staticFieldValues(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .peek(f -> f.setAccessible(true))
                .collect(
                        HashMap::new,
                        (m, f) -> {
                            try {
                                m.put(f.getName(), f.get(null));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        },
                        HashMap::putAll
                );
    }

    private static void compare(Map<String, Object> expected, Map<String, Object> actual) {
        expected.forEach((k, v) ->
                compare(v, actual.get(k), "Mismatch for field " + k));
        System.out.println("Static variables match");
    }

    private Class<?> reset(String className) throws ClassNotFoundException {
        unload();
        return loadClass(className);
    }

    private Map<String, Object> expectedStaticFieldValues = new HashMap<>();

    private ExecutionResult getInterpreted(String className)
            throws ClassNotFoundException, IOException {

        Class<?> clazz = reset(className);

        expectedStaticFieldValues = staticFieldValues(clazz);

        ExecutionResult interpretedResult = interpret(clazz);

        Files.write(
                Paths.get(dir).resolve(clazz.getName() + "_interpreted.txt"),
                interpretedResult.getOutput().getBytes()
        );

        return interpretedResult;
    }

    private ExecutionResult getCompiled(String className)
            throws ClassNotFoundException, IOException, NoSuchMethodException {

        Class<?> clazz = reset(className);

        Map<String, Object> actualStaticFieldValues =
                staticFieldValues(clazz);

        CompilationResult compiled = compile(clazz, false);
        ExecutionResult compiledResult = execute(compiled);

        // invalidate the compiled code
        compiled.invalidate();

        compare(expectedStaticFieldValues, actualStaticFieldValues);
        Files.write(
                Paths.get(dir).resolve(clazz.getName() + "_compiled.txt"),
                compiledResult.getOutput().getBytes()
        );

        return compiledResult;
    }

    private ExecutionResult getOptimized(String className)
            throws IOException, ClassNotFoundException, NoSuchMethodException {

        Class<?> clazz = reset(className);

        optimize(clazz);

        // reset the class again to prevent static fields from being modified
        clazz = reset(className);
        CompilationResult optimized = compile(clazz, true);
        ExecutionResult optimizedResult = execute(optimized);

        Files.write(
                Paths.get(dir).resolve(clazz.getName() + "_optimized.txt"),
                optimizedResult.getOutput().getBytes()
        );

        return optimizedResult;
    }

    private void compareResults(String className)
            throws IOException, ClassNotFoundException, NoSuchMethodException {
        // interpretation
        ExecutionResult interpreted = getInterpreted(className);

        // compilation
        ExecutionResult compiled = getCompiled(className);

        compare(interpreted, compiled);

        // optimization
        ExecutionResult optimized = getOptimized(className);

        compare(interpreted, optimized);
    }

    public static void main(String[] args) throws Throwable {

        logger.info("Initializing");
        HotSpotJVMCIRuntime jvmciRuntime = HotSpotJVMCIRuntime.runtime();
        HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) jvmciRuntime.getCompiler();
        HotSpotGraalRuntimeProvider graalRuntime = compiler.getGraalRuntime();
        HotSpotCodeCacheProvider codeCache = graalRuntime.getHostProviders().getCodeCache();
        logger.info("Runtime initialized");

        if (args.length == 0) {
            // if no arguments are provided,
            // compile and compare the sample classes

            for (int i = 0; i < REPETITIONS; i++) {

                // iterate over all class generation parameters
                for (Class<? extends Generative> desc : CLASSES) {

                    codeCache.resetCompilationStatistics();
                    log(desc, "%d", i);
                    CompileGeneratedClasses cgb = new CompileGeneratedClasses(OUTDIR, jvmciRuntime, compiler);

                    String className = cgb.generate(desc, i);

                    cgb.compareResults(className);
                }
            }
        } else {

            // otherwise do the same with the given class files
            for (String className : args) {
                codeCache.resetCompilationStatistics();
                CompileGeneratedClasses cgb = new CompileGeneratedClasses(".", jvmciRuntime, compiler);

                log(className, "Initialization");

                cgb.compareResults(className);
            }
        }
    }

}
