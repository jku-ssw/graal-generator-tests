package at.jku.ssw.java.bytecode.generator.tests.utils;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Contains formatting options.
 */
public final class Stringifier {
    private Stringifier() {
    }

    /**
     * Formats the given {@link ResolvedJavaMethod} in a human-readable
     * way and returns the resulting {@link String}.
     *
     * @param method The method that should be formatted
     * @return the {@link String} version of the method
     */
    public static String format(ResolvedJavaMethod method) {
        assert method != null;
        return method.format("%H.%n(%p):%r");
    }

}
