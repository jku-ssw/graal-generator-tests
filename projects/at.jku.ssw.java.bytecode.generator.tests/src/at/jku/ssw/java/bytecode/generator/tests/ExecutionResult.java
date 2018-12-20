package at.jku.ssw.java.bytecode.generator.tests;

class ExecutionResult {
    private final Object result;
    private final String output;
    private final Throwable exception;

    ExecutionResult(Object result,
                    String output,
                    Throwable exception) {
        this.result = result;
        this.output = output;
        this.exception = exception;
    }

    Object getResult() {
        return result;
    }

    String getOutput() {
        return output;
    }

    Throwable getException() {
        return exception;
    }
}
