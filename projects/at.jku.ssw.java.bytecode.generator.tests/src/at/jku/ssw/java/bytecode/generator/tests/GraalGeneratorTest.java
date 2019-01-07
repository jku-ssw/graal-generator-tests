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
import org.apache.commons.cli.*;
import org.graalvm.compiler.hotspot.HotSpotGraalCompiler;
import org.graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
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

    /**
     * The threshold to get a suitable amount of information to produce
     * optimized results during the optimized compilation step.
     */
    private final int optimizationThreshold;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Creates a new Graal tester.
     *
     * @param outDir                The directory where the generated classes
     *                              should be stored in
     * @param runtime               The runtime reference
     * @param compiler              The Graal compiler reference
     * @param optimizationThreshold The test runs to execute to get optimization information
     */
    private GraalGeneratorTest(String outDir,
                               HotSpotJVMCIRuntime runtime,
                               HotSpotGraalCompiler compiler,
                               int optimizationThreshold) {
        this.workingDirectory = Paths.get(outDir);
        this.runtime = runtime;
        this.compiler = compiler;
        this.optimizationThreshold = optimizationThreshold;
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
                optimizationThreshold
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

        int repetitions;
        int optimizationThreshold;

        Options commandLineOptions = getCommandLineOptions();
        CommandLine commandLine = getCommandLine(commandLineOptions, args);

        if (commandLine.hasOption("help")) {
            // only show the help dialog if this argument is passed
            printCommandLineHelp(commandLineOptions);
            return;
        }

        repetitions = Optional
                .ofNullable(commandLine.getParsedOptionValue("r"))
                .map(Number.class::cast)
                .map(Number::intValue)
                .orElse(REPETITIONS);

        if (repetitions <= 0)
            throw new IllegalArgumentException("The number of repetitions must be greater than 0");

        optimizationThreshold = Optional
                .ofNullable(commandLine.getParsedOptionValue("o"))
                .map(Number.class::cast)
                .map(Number::intValue)
                .orElse(OPTIMIZATION_THRESHOLD);

        if (optimizationThreshold < 0)
            throw new IllegalArgumentException("The optimization threshold must at least be 0");

        // the remaining arguments are potential class files
        String[] classFiles = commandLine.getArgs();

        if (classFiles.length == 0) {
            // if no more arguments are provided,
            // generate, compile and compare the sample classes

            final Path outDir = Paths.get(OUTDIR);

            // ensure that the path is valid
            if (!Files.exists(outDir))
                Files.createDirectories(outDir);
            else if (!Files.isDirectory(outDir))
                throw new IllegalArgumentException(outDir + " does not denote a valid directory");


            // iterate over all class generation parameters
            new ClassFileGenerator(outDir, repetitions).forEach(name -> {
                GraalGeneratorTest cgb = new GraalGeneratorTest(
                        OUTDIR,
                        jvmciRuntime,
                        compiler,
                        optimizationThreshold
                );

                try {
                    cgb.testGraalCompilerForClass(name);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            // otherwise do the same with the given class files
            Arrays.stream(classFiles)
                    // strip possible ".class" extensions
                    .map(f -> f.endsWith(".class")
                            ? f.substring(0, f.length() - 6)
                            : f)
                    .forEach(className -> {
                        codeCache.resetCompilationStatistics();
                        GraalGeneratorTest cgb = new GraalGeneratorTest(
                                ".",
                                jvmciRuntime,
                                compiler,
                                optimizationThreshold
                        );

                        logger.log(Level.WARNING, className + " - Initialization");

                        try {
                            cgb.testGraalCompilerForClass(className);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    // endregion
    //-------------------------------------------------------------------------
    // region CLI utilities

    /**
     * Static helper that defines the allowed command line options.
     *
     * @return the individual command line options that this application
     * supports
     */
    private static Options getCommandLineOptions() {
        return new Options()
                .addOption(
                        Option.builder("r")
                                .longOpt("repetitions")
                                .desc("The number of times each class template should be generated")
                                .hasArg(true)
                                .required(false)
                                .type(Number.class)
                                .build()
                )
                .addOption(
                        Option.builder("o")
                                .longOpt("optimization-threshold")
                                .desc("The number of times a class should be run before compiling it with optimization information")
                                .hasArg(true)
                                .required(false)
                                .type(Number.class)
                                .build()
                )
                .addOption(
                        Option.builder("h")
                                .longOpt("help")
                                .desc("Shows the command line overview")
                                .hasArg(false)
                                .required(false)
                                .build()
                );
    }

    /**
     * Parses the command line arguments and generates a wrapper to
     * simplify checking for individual values.
     *
     * @param options The command line options that are defined for this application
     * @param args    The arguments that were given to the program
     * @return a command line instance
     */
    private static CommandLine getCommandLine(Options options, String[] args) throws ParseException {
        return new DefaultParser().parse(options, args);
    }

    /**
     * Shows an overview of all supported command line arguments and their
     * usage.
     *
     * @param options The command line options that should be printed
     */
    private static void printCommandLineHelp(Options options) {
        new HelpFormatter().printHelp("graal_generator_tests", options, true);
    }

    // endregion
    //-------------------------------------------------------------------------

}
