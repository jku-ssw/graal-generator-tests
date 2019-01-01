package at.jku.ssw.java.bytecode.generator.tests.utils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Utilities that extend the functionality of {@link java.lang.reflect}.
 */
public class ReflectUtils {

    /**
     * The name of the main method.
     */
    public static final String MAIN_METHOD = "main";

    /**
     * Safe method that determines the main method of the given class.
     *
     * @param clazz The target class
     * @return a reference to the main method of the class or nothing if the
     * class has no main method
     */
    public static Optional<Method> mainMethodOf(Class<?> clazz) {
        try {
            // look up the `main([Ljava.lang.String;)V` method
            return Optional.of(clazz.getMethod(MAIN_METHOD, String[].class));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}
