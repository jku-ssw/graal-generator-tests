package at.jku.ssw.java.bytecode.generator.tests.utils;

import at.jku.ssw.java.bytecode.generator.tests.results.ExecutionResult;

import java.io.*;
import java.util.function.Supplier;

/**
 * Provides helpers to execute tasks and redirect output channels.
 */
public final class Executor {
    private Executor() {
    }

    /**
     * Runs the given supplier and stores the result as well as any output
     * that was printed to STDOUT or STDERR.
     *
     * @param task The task to run
     * @return The result of the task (including exceptions and output)
     * @throws IOException if the output cannot be captured
     */
    public static ExecutionResult captureIO(Supplier<Object> task)
            throws IOException {

        // store the previous IO settings
        final PrintStream outDefault = System.out;
        final PrintStream errDefault = System.err;

        // the actual result
        Object result = null;

        // the captured output
        String out;

        // any thrown exception
        Throwable exception = null;

        // create a print stream that writes to a byte array
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             PrintStream ps = new PrintStream(os)) {

            // redirect STDOUT and STDERR to the custom print stream
            System.setOut(ps);
            System.setErr(ps);

            // run the supplier
            result = task.get();

            // and get the output
            out = os.toString();
        } catch (RuntimeException e) {
            // capture the stack trace
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            out = w.toString();
            exception = e;
        }

        // reset the IO channels
        System.setErr(errDefault);
        System.setOut(outDefault);

        return new ExecutionResult(result, out, exception);
    }
}
