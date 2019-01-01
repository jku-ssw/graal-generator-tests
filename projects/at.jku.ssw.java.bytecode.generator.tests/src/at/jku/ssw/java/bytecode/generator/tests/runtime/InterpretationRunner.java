package at.jku.ssw.java.bytecode.generator.tests.runtime;

import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;
import at.jku.ssw.java.bytecode.generator.tests.utils.Executor;
import at.jku.ssw.java.bytecode.generator.tests.utils.StaticFieldGuard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static at.jku.ssw.java.bytecode.generator.tests.utils.ReflectUtils.mainMethodOf;

/**
 * {@link ClassRunner} that interprets a class' method
 */
public class InterpretationRunner extends ClassRunner {

    //-------------------------------------------------------------------------
    // region Logging

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(InterpretationRunner.class.getName());

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Creates a new interpreter wrapper.
     *
     * @param workingDirectory The working directory where class files are
     *                         loaded from
     * @param guard            Optional field guard
     */
    public InterpretationRunner(Path workingDirectory, StaticFieldGuard guard) {
        super(workingDirectory, guard);
    }

    /**
     * @see #InterpretationRunner(Path, StaticFieldGuard)
     */
    public InterpretationRunner(Path workingDirectory) {
        super(workingDirectory);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Overridden methods

    /**
     * {@inheritDoc}
     */
    @Override
    protected final ExecutionResult call(Class<?> clazz) throws IOException {
        info(clazz, "Interpreting code");
        try {
            return Executor.captureIO(
                    () -> mainMethodOf(clazz)
                            .map(main -> {
                                try {
                                    return main.invoke(null, (Object) new String[0]);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .orElse(null)
            );
        } catch (Throwable t) {
            fail(clazz, t, "HotSpot interpretation failed");
            throw t;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger logger() {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String outName(Class<?> clazz) {
        return clazz.getName() + "_interpreted.txt";
    }

    // endregion
    //-------------------------------------------------------------------------
}
