package at.jku.ssw.java.bytecode.generator.tests.classes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

public abstract class Generative {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CLIArguments {
        String[] args() default {};

        int iterations();
    }

    /**
     * Returns the command line arguments that are defined by the given
     * {@link Generative} class for the given iteration.
     *
     * @param clazz The class that is generated and which specifies
     *              the arguments
     * @param iter  The current iteration (to adapt the class name accordingly)
     * @return an array of strings that describe the individual command
     * line argument parts (key-value pairs are also split up into individual
     * arguments)
     */
    public static String[] args(Class<? extends Generative> clazz, int iter) {
        CLIArguments args = clazz.getAnnotation(CLIArguments.class);

        return Stream.concat(
                Stream.of(
                        "-l", String.valueOf(args.iterations()),                    // use `iters` iterations to generate the class
                        "-filename", clazz.getSimpleName() + iter   // use file name
                ),
                Stream.of(args.args())
        ).toArray(String[]::new);
    }
}
