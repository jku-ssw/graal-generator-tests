package at.jku.ssw.java.bytecode.generator.tests.utils;

import at.jku.ssw.java.bytecode.generator.tests.comparison.Comparing;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Guard that ensures that the static fields of a class are not mutated
 * in between test runs. Since that class (and class loader) should
 * be reloaded, changes to the fields should not influence subsequent runs.
 * This class provides additional utilities to check this condition.
 */
public final class StaticFieldGuard implements Comparing {

    //-------------------------------------------------------------------------
    // region Logging

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(StaticFieldGuard.class.getName());

    // endregion
    //-------------------------------------------------------------------------
    // region Properties

    /**
     * Contains the name and value pairs of the static fields of the class.
     */
    private final Map<String, Object> fields;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Creates a new {@link StaticFieldGuard} for the given field settings.
     *
     * @param fields The field assignment
     */
    private StaticFieldGuard(Map<String, Object> fields) {
        this.fields = fields;
    }

    /**
     * Creates a new static guard for the properties of the given class.
     *
     * @param clazz The target class
     * @return a new {@link StaticFieldGuard} that is bound to the given
     * version of the class.
     */
    public static StaticFieldGuard forClass(Class<?> clazz) {
        return new StaticFieldGuard(getStaticFieldValues(clazz));
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Instance methods

    /**
     * Verifies that the field values still hold for the given class.
     *
     * @param clazz The class that is checked
     */
    public void verify(Class<?> clazz) {
        Map<String, Object> newFields = getStaticFieldValues(clazz);

        fields.forEach((k, v) ->
                compare(v, newFields.get(k), "Mismatch for field " + k));

        logger.info("Static variables match");
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Private helpers

    /**
     * Helper that allows to check all static field value of a particular
     * class.
     *
     * @param clazz The class instance whose fields are evaluated
     * @return a map containing the field name and the corresponding value
     */
    private static Map<String, Object> getStaticFieldValues(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .peek(f -> f.setAccessible(true))
                .collect(
                        HashMap::new,
                        (m, f) -> {
                            try {
                                m.put(f.getName(), f.get(null));
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        HashMap::putAll
                );
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Overridden methods

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
