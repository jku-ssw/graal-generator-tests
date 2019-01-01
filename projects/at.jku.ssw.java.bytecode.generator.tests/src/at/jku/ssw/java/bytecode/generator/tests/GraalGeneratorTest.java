package at.jku.ssw.java.bytecode.generator.tests;

import at.jku.ssw.java.bytecode.generator.tests.comparison.Comparing;
import at.jku.ssw.java.bytecode.generator.tests.generation.ClassFileGenerator;
import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;
import at.jku.ssw.java.bytecode.generator.tests.runtime.CompiledRunner;
import at.jku.ssw.java.bytecode.generator.tests.runtime.InterpretationRunner;
import at.jku.ssw.java.bytecode.generator.tests.runtime.OptimizedRunner;
import at.jku.ssw.java.bytecode.generator.tests.utils.StaticFieldGuard;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import org.graalvm.compiler.hotspot.HotSpotGraalCompiler;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraalGeneratorTest implements Comparing {
    //-------------------------------------------------------------------------
    // region Constants

    /**
     * The default output directory for generated classes.
     */
    private static final String OUTDIR = "./generated_classes";

    /**
     * The number of repetitions that should be run.
     */
    private static final int REPETITIONS = 100;

    /**
     * The number of times that classes should be run to get a suitable amount
     * of information for the optimization step.
     */
    private static final int OPTIMIZATION_THRESHOLD = 10;

    // endregion
    //-------------------------------------------------------------------------
    // region Logging

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(GraalGeneratorTest.class.getName());

    // endregion
    //-------------------------------------------------------------------------
    // region Properties

    /**
     * The current working directory.
     */
    private final Path workingDirectory;

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
     * Creates a new Graal tester.
     *
     * @param outDir   The directory where the generated classes
     *                 should be stored in
     * @param runtime  The runtime reference
     * @param compiler The Graal compiler reference
     */
    private GraalGeneratorTest(String outDir,
                               HotSpotJVMCIRuntime runtime,
                               HotSpotGraalCompiler compiler) {
        this.workingDirectory = Paths.get(outDir);
        this.runtime = runtime;
        this.compiler = compiler;
    }


    // endregion
    //-------------------------------------------------------------------------
    // region Instance methods

    /**
     * Runs the class file that is identified by the given name
     * in both the interpreter and as a compiled result and compares the
     * results.
     *
     * @param className The name of the class that is run
     * @throws Exception if the execution fails or the class name is invalid
     */
    private void testGraalCompilerForClass(String className) throws Exception {
        assert className != null;

        // create an interpreter
        // (no field guard since this is the initial step)
        InterpretationRunner interpreter = new InterpretationRunner(
                workingDirectory
        );

        // interpretation
        ExecutionResult interpreted = interpreter.call(className);

        // get the field guard
        StaticFieldGuard guard = interpreter.guard();

        CompiledRunner compiler = new CompiledRunner(
                workingDirectory,
                guard,
                runtime,
                this.compiler
        );


        // compilation
        ExecutionResult compiled = compiler.call(className);

        compareResults(interpreted, compiled);

        OptimizedRunner optimizer = new OptimizedRunner(
                workingDirectory,
                guard,
                runtime,
                this.compiler,
                OPTIMIZATION_THRESHOLD
        );

        // optimization
        ExecutionResult optimized = optimizer.call(className);

        compareResults(interpreted, optimized);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Overridden methods

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger logger() {
        return logger;
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Main entry point

    public static void main(String[] args) throws Throwable {

        logger.info("Initializing");
        // Retrieve the Graal runtime / compiler references
        HotSpotJVMCIRuntime jvmciRuntime = HotSpotJVMCIRuntime.runtime();
        HotSpotGraalCompiler compiler = (HotSpotGraalCompiler) jvmciRuntime.getCompiler();
        HotSpotGraalRuntimeProvider graalRuntime = compiler.getGraalRuntime();
        HotSpotCodeCacheProvider codeCache = graalRuntime.getHostProviders().getCodeCache();
        logger.info("Runtime initialized");

        if (args.length == 0) {
            // if no arguments are provided,
            // compile and compare the sample classes

            final Path outDir = Paths.get(OUTDIR);

            // ensure that the path is valid
            if (!Files.exists(outDir))
                Files.createDirectories(outDir);
            else if (!Files.isDirectory(outDir))
                throw new IllegalArgumentException(outDir + " does not denote a valid directory");


            for (int i = 0; i < REPETITIONS; i++) {

                // iterate over all class generation parameters
                new ClassFileGenerator(outDir, REPETITIONS).forEach(name -> {
                    GraalGeneratorTest cgb = new GraalGeneratorTest(
                            OUTDIR,
                            jvmciRuntime,
                            compiler
                    );

                    try {
                        cgb.testGraalCompilerForClass(name);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } else {
            // otherwise do the same with the given class files
            for (String name : args) {
                codeCache.resetCompilationStatistics();
                GraalGeneratorTest cgb = new GraalGeneratorTest(
                        ".",
                        jvmciRuntime,
                        compiler
                );

                logger.log(Level.WARNING, name + " - Initialization");

                cgb.testGraalCompilerForClass(name);
            }
        }
    }

    // endregion
    //-------------------------------------------------------------------------

}
