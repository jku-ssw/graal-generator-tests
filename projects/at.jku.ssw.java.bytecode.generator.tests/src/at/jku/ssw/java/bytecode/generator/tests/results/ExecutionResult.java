package at.jku.ssw.java.bytecode.generator.tests.results;

public class ExecutionResult {
    private final Object result;
    private final String output;
    private final Throwable exception;

    public ExecutionResult(Object result,
                           String output,
                           Throwable exception) {
        this.result = result;
        this.output = output;
        this.exception = exception;
    }

    public Object result() {
        return result;
    }

    public String output() {
        return output;
    }

    public Throwable exception() {
        return exception;
    }
}
