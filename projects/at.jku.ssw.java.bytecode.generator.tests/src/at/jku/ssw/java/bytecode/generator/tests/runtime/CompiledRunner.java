package at.jku.ssw.java.bytecode.generator.tests.runtime;

import at.jku.ssw.java.bytecode.generator.tests.results.CompilationResult;
import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;
import at.jku.ssw.java.bytecode.generator.tests.utils.Executor;
import at.jku.ssw.java.bytecode.generator.tests.utils.StaticFieldGuard;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCICompiler;
import org.graalvm.collections.EconomicMap;
import org.graalvm.compiler.bytecode.Bytecodes;
import org.graalvm.compiler.hotspot.CompilationTask;
import org.graalvm.compiler.hotspot.HotSpotGraalCompiler;
import org.graalvm.compiler.options.OptionValues;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static at.jku.ssw.java.bytecode.generator.tests.utils.Stringifier.format;

/**
 * A compiler wrapper that uses Graal to compile a given class' methods
 * and then executes it
 */
public class CompiledRunner extends ClassRunner {

    //-------------------------------------------------------------------------
    // region Logging

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(CompiledRunner.class.getName());

    // endregion
    //-------------------------------------------------------------------------
    // region Properties

    /**
     * The compiler runtime reference that should be used for compilation.
     */
    private final HotSpotJVMCIRuntime runtime;

    /**
     * The Graal compiler reference that is used for compilation.
     */
    private final HotSpotGraalCompiler compiler;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Initializes a new compiler wrapper.
     *
     * @param workingDirectory The working directory from which class files
     *                         are loaded
     * @param guard            Optional field guard
     * @param runtime          The runtime reference
     * @param compiler         The Graal compiler reference
     */
    public CompiledRunner(Path workingDirectory,
                          StaticFieldGuard guard,
                          HotSpotJVMCIRuntime runtime,
                          HotSpotGraalCompiler compiler) {
        super(workingDirectory, guard);
        this.runtime = runtime;
        this.compiler = compiler;
    }

    /**
     * @see #CompiledRunner(Path, StaticFieldGuard, HotSpotJVMCIRuntime, HotSpotGraalCompiler)
     */
    public CompiledRunner(Path workingDirectory,
                          HotSpotJVMCIRuntime runtime,
                          HotSpotGraalCompiler compiler) {
        this(workingDirectory, null, runtime, compiler);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Compilation methods

    /**
     * Executes the given compiled result and captures any output.
     *
     * @param compiled The compiled class
     * @return the result of the execution
     * @throws IOException if the compiled code is invalid
     */
    protected final ExecutionResult execute(CompilationResult compiled) throws IOException {
        info(compiled.clazz(), "Executing compiled code");
        try {
            return Executor.captureIO(
                    () -> {
                        try {
                            return compiled.main().executeVarargs((Object) new String[0]);
                        } catch (InvalidInstalledCodeException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
            );
        } catch (Throwable t) {
            fail(compiled.clazz(), t, "HotSpot interpretation failed");
            throw t;
        }
    }

    /**
     * Compiles all methods of the given class.
     *
     * @param clazz            The class whose methods are compiled
     * @param useProfilingInfo Flag that indicates whether optimization
     *                         information should be used for compilation
     * @return the result of the compilation process that references the
     * installed code of each method
     * @throws NoSuchMethodException if a method cannot be resolved correctly
     */
    protected final CompilationResult compile(Class<?> clazz, boolean useProfilingInfo) throws NoSuchMethodException {
        info(clazz, "Compilation");
        try {
            // load and initialize the class
            MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();

            info(clazz, "Pre-loading constant pool classes");
            // pre-load all classes in the constant pool.
            try {
                HotSpotResolvedObjectType objectType = (HotSpotResolvedObjectType) metaAccess.lookupJavaType(clazz);
                ConstantPool constantPool = objectType.getConstantPool();
                for (int cpi = 1; cpi < constantPool.length(); cpi++)
                    constantPool.loadReferencedType(cpi, Bytecodes.LDC);

            } catch (Throwable t) {
                // if something went wrong during pre-loading we just ignore it.
                fail(clazz, t, "Pre-loading failed");
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

            // concat all methods and constructors and compile them
            // here also class initializers are included
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

            return new CompilationResult(clazz, main, others);
        } catch (Throwable t) {
            fail(clazz, t, "Error in compilation %s");
            throw t;
        }
    }

    /**
     * Compiles the given resolved method of the given class.
     *
     * @param clazz            The class that contains the target method
     * @param method           The method that should be compiled
     * @param useProfilingInfo Flag that indicates whether optimization
     *                         information should be used
     * @return the installed code of the method or nothing if an exception is
     * thrown when compiling it
     */
    protected final Optional<InstalledCode> compile(Class<?> clazz,
                                                    HotSpotResolvedJavaMethod method,
                                                    boolean useProfilingInfo) {

        info(clazz, "Compiling method %s", format(method));

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
            fail(clazz, t, "Error compiling method: %s", format(method));
            return Optional.empty();
        }
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Overridden methods

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExecutionResult call(Class<?> clazz) throws Exception {
        CompilationResult compiled = compile(clazz, false);
        ExecutionResult compiledResult = execute(compiled);

        // invalidate the compiled code
        compiled.main().invalidate();
        compiled.others().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(InstalledCode::invalidate);

        return compiledResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String outName(Class<?> clazz) {
        return clazz.getName() + "_compiled.txt";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger logger() {
        return logger;
    }

    // endregion
    //-------------------------------------------------------------------------

}
