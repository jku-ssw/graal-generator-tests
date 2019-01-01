package at.jku.ssw.java.bytecode.generator.tests.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides helpers to easily log messages for specific classes and
 * error messages.
 */
public interface Logging {
    //-------------------------------------------------------------------------
    // region Abstract methods

    /**
     * @return the default logger instance to use when printing messages
     */
    Logger logger();

    // endregion
    //-------------------------------------------------------------------------
    // region Default methods

    /**
     * Prints the given message using the given arguments.
     *
     * @param message The message that is printed
     * @param args    The arguments that are embedded
     */
    default void info(String message, Object... args) {
        logger().info(String.format(message, args));
    }

    /**
     * Prints the given message and prepends the class name.
     *
     * @param clazz   The class that reported the given message
     * @param message The message
     * @param args    The arguments that are embedded
     */
    default void info(Class<?> clazz, String message, Object... args) {
        info(clazz.getSimpleName() + " : " + message, args);
    }

    /**
     * @see #info(Class, String, Object...)
     */
    default void info(String className, String message, Object... args) {
        info(className + " : " + message, args);
    }

    /**
     * Prints the given error message and embeds the given arguments.
     * Also appends the exception trace.
     *
     * @param throwable The thrown exception
     * @param message   The string that may be a pattern
     * @param args      The arguments that are embedded
     */
    default void fail(Throwable throwable, String message, Object... args) {
        logger().log(Level.SEVERE, String.format(message, args), throwable);
    }

    /**
     * Prints the given string as an error message.
     *
     * @param message The string that may be a pattern
     * @param args    The arguments that are embedded
     */
    default void fail(String message, Object... args) {
        logger().severe(String.format(message, args));
    }

    /**
     * Reports an error message and associates the given class with it.
     *
     * @param clazz     The class that caused the error
     * @param throwable The exception that was reported
     * @param message   The string that may be a pattern
     * @param args      The arguments that are embedded
     */
    default void fail(Class<?> clazz,
                      Throwable throwable,
                      String message,
                      Object... args) {
        fail(throwable, clazz.getSimpleName() + " : " + message, throwable, args);
    }

    // endregion
    //-------------------------------------------------------------------------

}
