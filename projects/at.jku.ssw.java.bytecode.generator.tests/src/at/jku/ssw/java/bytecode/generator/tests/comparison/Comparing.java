package at.jku.ssw.java.bytecode.generator.tests.comparison;

import at.jku.ssw.java.bytecode.generator.tests.logging.Logging;
import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;

import java.util.logging.Logger;

/**
 * Provides methods for comparing results and states.
 */
public interface Comparing extends Logging {
    //-------------------------------------------------------------------------
    // region Constants

    /**
     * The logger pattern for comparison failures.
     */
    String FAIL_PATTERN = "%s - Expected: %s -- Actual: %s";

    // endregion
    //-------------------------------------------------------------------------
    // region Abstract methods

    /**
     * Retrieve the assigned logger.
     *
     * @return the logger instance that should be used when printing messages.
     */
    Logger logger();

    // endregion
    //-------------------------------------------------------------------------
    // region Default methods

    /**
     * Logs a comparison failure but does not throw anything.
     * Any critical failure must therefore be manually tested to ensure
     * an ordered shutdown or exit when encountering errors.
     *
     * @param expected The expected result
     * @param actual   The actual result
     * @param message  The message that is printed
     *                 (in addition to the expected / actual values)
     * @param <T>      the type of the compared objects
     */
    default <T> void failComparison(T expected, T actual, String message) {
        fail(FAIL_PATTERN, message, expected, actual);
    }

    /**
     * Compare the given values.
     * If the given types are reference types, they are assumed to be equal,
     * if their {@link Object#equals(Object)} call returns {@code true}.
     *
     * @param expected The expected value
     * @param actual   The actual value
     * @param message  The message to print on failure
     * @param <T>      The type of the compared values
     * @return {@code true} if the values are equals; {@code false} otherwise
     */
    default <T> boolean compare(T expected, T actual, String message) {
        // if both are the same, they are equal
        if (expected == actual)
            return true;

        // if one of them is null, but they are not the same,
        // it is an error
        if (expected == null) {
            failComparison(null, actual, message);
            return false;
        }

        if (actual == null) {
            failComparison(expected, null, message);
            return false;
        }

        // if both are not null, use Object#equals
        if (!expected.equals(actual)) {
            failComparison(expected, actual, message);
            return false;
        }

        return true;
    }

    /**
     * Compares two {@link ExecutionResult}s and lots any mismatches.
     *
     * @param expected The expected result
     * @param actual   The actual result
     */
    default void compareResults(ExecutionResult expected, ExecutionResult actual) {
        // check if the result should contain an exception
        if (expected.exception() != null) {
            // if so, ensure that the actual result also contains one
            if (compare(expected.exception(), actual.exception(), "Expected exception")) {
                // if so, compare the exception types
                // (e.g. ArrayOutOfBoundsException vs. NullPointerException)
                compare(expected.exception().getClass(), actual.exception().getClass(), "Wrong exception type");
                // compare the actual messages (for same exceptions for different occasions - e.g. IOException)
                compare(expected.exception().getMessage(), actual.exception().getMessage(), "Wrong exception message");
            }
        } else {
            // ensure that the actual result also does not contain an exception
            compare(null, actual.exception(), "Unexpected exception");

            // if the result should not be null, compare them directly
            if (expected.result() != null)
                compare(expected.result(), actual.result(), "Wrong result");
            else
                // otherwise ensure that also the actual result is null
                compare(null, actual.result(), "Unexpected result");

            // extract the (expected / actual) output (of STDIN)
            // and consider the individual lines
            String[] linesExpected = expected.output().split(System.lineSeparator());
            String[] linesActual = actual.output().split(System.lineSeparator());

            // compare the output lines
            for (int i = 0; i < linesExpected.length || i < linesActual.length; i++) {
                // ensure that both outputs have equal number of lines
                if (i >= linesExpected.length)
                    // otherwise note the additional output
                    compareLines("", linesActual[i], i);
                else if (i >= linesActual.length)
                    // if the given output is too short, it is also noted
                    compareLines(linesExpected[i], "", i);
                else
                    // otherwise compare the lines directly
                    compareLines(linesExpected[i], linesActual[i], i);
            }
        }
    }

    /**
     * Simple {@link String} line comparison.
     *
     * @param expected The expected output line
     * @param actual   The actual output line
     * @param line     The line number (for the output)
     */
    default void compareLines(String expected, String actual, int line) {
        compare(expected, actual, "Line " + line);
    }

    // endregion
    //-------------------------------------------------------------------------
}
