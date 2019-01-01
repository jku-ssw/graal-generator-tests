package at.jku.ssw.java.bytecode.generator.tests.runtime;

import at.jku.ssw.java.bytecode.generator.loaders.GeneratedClassLoader;
import at.jku.ssw.java.bytecode.generator.tests.logging.Logging;
import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;
import at.jku.ssw.java.bytecode.generator.tests.utils.StaticFieldGuard;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for all executing classes (e.g. interpretation, compilation)
 * that defines the common properties like the working directory and an
 * optional field guard.
 */
public abstract class ClassRunner implements Logging {

    //-------------------------------------------------------------------------
    // region Properties

    /**
     * The working directory where the class files are located.
     */
    private final Path workingDirectory;

    /**
     * Optional field guard that is either verified on each run or set.
     */
    private StaticFieldGuard guard;

    /**
     * The current class loader instance.
     * This must be mutable since the class loader is reset for certain runs.
     */
    private GeneratedClassLoader classLoader;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Creates a new class.
     *
     * @param workingDirectory The directory from which potential class files
     *                         are loaded
     * @param guard            The guard that keeps track of static field
     *                         values
     */
    protected ClassRunner(Path workingDirectory, StaticFieldGuard guard) {
        this.workingDirectory = workingDirectory;
        this.guard = guard;
    }

    /**
     * @see #ClassRunner(Path, StaticFieldGuard)
     */
    protected ClassRunner(Path workingDirectory) {
        this(workingDirectory, null);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Concrete methods / utilities

    /**
     * Resets the class loader and forces a garbage collection.
     */
    public final void unload() {
        classLoader = new GeneratedClassLoader(workingDirectory.toString());
        System.gc();
    }

    /**
     * Reloads the given class by unloading the class loader and reloading
     * the class file.
     *
     * @param className The name of the class that is reloaded
     * @return the loaded class instance
     * @throws ClassNotFoundException if the class name does not denote
     *                                a valid class file
     */
    protected final Class<?> reloadClass(String className) throws ClassNotFoundException {
        unload();
        return classLoader.loadClass(className);
    }

    /**
     * Calls this class runner to execute the class that is identified
     * by the given name and whose class files should exist in the working
     * directory.
     *
     * @param className The class that is run
     * @return the result of the execution of the class
     * @throws Exception if the class may not be loaded or the execution fails
     */
    public final ExecutionResult call(String className) throws Exception {
        // retrieve the class and reset any possible state
        Class<?> clazz = reloadClass(className);

        // either set or verify the guard
        if (guard == null)
            guard = StaticFieldGuard.forClass(clazz);
        else
            guard.verify(clazz);

        ExecutionResult result = call(clazz);

        // write the output to another file
        Files.write(
                workingDirectory.resolve(outName(clazz)),
                result.output().getBytes()
        );

        return result;
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Abstract methods

    /**
     * Executes the given class.
     * The method of execution is implementation specific
     *
     * @param clazz The class that should be executed
     * @return the result of the execution
     * @throws Exception if the call fails
     */
    protected abstract ExecutionResult call(Class<?> clazz) throws Exception;

    /**
     * Generates the output name for the captured STDOUT and STDERR
     * for the given class. When executed, the output is written to a file
     * of this name in the output directory.
     *
     * @param clazz The class that is executed.
     * @return the name of the targeted output file
     */
    protected abstract String outName(Class<?> clazz);

    // endregion
    //-------------------------------------------------------------------------
    // region Getters / setters

    /**
     * @return the current working directory
     */
    public Path workingDirectory() {
        return workingDirectory;
    }

    /**
     * @return the current field guard (or {@code null} if none is set)
     */
    public StaticFieldGuard guard() {
        return guard;
    }

    /**
     * Resets the current guard (if any).
     */
    public void resetGuard() {
        this.guard = null;
    }

    // endregion
    //-------------------------------------------------------------------------
}
