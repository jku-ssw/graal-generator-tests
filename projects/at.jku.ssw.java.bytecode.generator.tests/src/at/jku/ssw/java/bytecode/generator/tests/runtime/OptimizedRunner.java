package at.jku.ssw.java.bytecode.generator.tests.runtime;

import at.jku.ssw.java.bytecode.generator.tests.results.CompilationResult;
import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;
import at.jku.ssw.java.bytecode.generator.tests.utils.StaticFieldGuard;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import org.graalvm.compiler.hotspot.HotSpotGraalCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Special {@link CompiledRunner} implementation that first gathers information
 * about a class and then compiles it using optimization techniques.
 */
public class OptimizedRunner extends CompiledRunner {

    //-------------------------------------------------------------------------
    // region Logging

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(OptimizedRunner.class.getName());

    // endregion
    //-------------------------------------------------------------------------
    // region Properties

    /**
     * The number of times each class should be run to gather information
     * for the optimizer.
     */
    private final int optimizationRuns;

    /**
     * The interpreter that is used to gather optimization information.
     */
    private final InterpretationRunner interpreter;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Initializes a new optimizer.
     *
     * @param workingDirectory The working directory from which class files
     *                         are loaded
     * @param guard            Optional field guard
     * @param runtime          The runtime reference
     * @param compiler         The compiler reference
     * @param optimizationRuns The number of optimization runs that should
     *                         occur
     */
    public OptimizedRunner(Path workingDirectory,
                           StaticFieldGuard guard,
                           HotSpotJVMCIRuntime runtime,
                           HotSpotGraalCompiler compiler,
                           int optimizationRuns) {
        super(workingDirectory, guard, runtime, compiler);
        this.optimizationRuns = optimizationRuns;
        interpreter = new InterpretationRunner(workingDirectory, guard);
    }

    /**
     * @see #OptimizedRunner(Path, StaticFieldGuard, HotSpotJVMCIRuntime, HotSpotGraalCompiler, int)
     */
    public OptimizedRunner(Path workingDirectory,
                           HotSpotJVMCIRuntime runtime,
                           HotSpotGraalCompiler compiler,
                           int optimizationRuns) {
        this(workingDirectory, null, runtime, compiler, optimizationRuns);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Execution / optimization

    /**
     * Gathers information about the given class by interpreting it
     * a number of times.
     *
     * @param clazz The class that should be analyzed
     * @return a list of all results
     * @throws IOException if the class cannot be loaded
     */
    private List<ExecutionResult> optimize(Class<?> clazz) throws IOException {
        List<ExecutionResult> r = new ArrayList<>();
        for (int i = 0; i < optimizationRuns; i++) {
            r.add(interpreter.call(clazz));
        }
        return r;
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Overridden methods

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExecutionResult call(Class<?> clazz) throws Exception {
        // performs a number of optimization runs
        @SuppressWarnings("unused")
        List<ExecutionResult> results = optimize(clazz);

        // reset the class again to prevent static fields from being modified
        clazz = reloadClass(clazz.getName());

        // finally compile the result using optimization information
        CompilationResult optimized = compile(clazz, true);
        return execute(optimized);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String outName(Class<?> clazz) {
        return clazz.getName() + "_optimized.txt";
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
