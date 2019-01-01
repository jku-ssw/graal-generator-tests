package at.jku.ssw.java.bytecode.generator.tests.generation;

import at.jku.ssw.java.bytecode.generator.cli.ControlValueParser;
import at.jku.ssw.java.bytecode.generator.cli.GenerationController;
import at.jku.ssw.java.bytecode.generator.generators.RandomCodeGenerator;
import at.jku.ssw.java.bytecode.generator.tests.generation.classes.*;
import at.jku.ssw.java.bytecode.generator.tests.logging.Logging;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * A generator that allows for repeated generation of a limited number of
 * predefined class types.
 * It is iterable where each iteration lazily generates a new class file
 * and returns the file location.
 */
public class ClassFileGenerator implements Logging, Iterable<String> {

    //-------------------------------------------------------------------------
    // region Logging

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ClassFileGenerator.class.getName());

    // endregion
    //-------------------------------------------------------------------------
    // region Constants

    /**
     * The class types that may be generated.
     */
    private static final List<Class<? extends Generative>> CLASS_TYPES =
            Arrays.asList(
                    SimpleClass.class,
                    ComplexClass.class,
                    LotsOfMath.class,
//                    Overflows.class,
//                    DivisionsByZero.class,
                    ManyLoops.class,
                    BlockExits.class,
                    ManyOverloads.class,
                    HighBranchingFactor.class
            );

    // endregion
    //-------------------------------------------------------------------------
    // region Properties

    /**
     * The working directory where each class file is generated in.
     */
    private final Path workingDirectory;

    /**
     * The number of times each class type should be generated.
     */
    private final int iterations;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Creates a new generator using the given working directory.
     *
     * @param workingDirectory The path at which all class file are generated
     * @param iterations       The iteration count
     */
    public ClassFileGenerator(Path workingDirectory, int iterations) {
        this.workingDirectory = workingDirectory;
        this.iterations = iterations;
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Generator methods

    /**
     * Creates a class file in the working directory using the given class
     * type as a template and the iteration counter for naming.
     *
     * @param clazz The class type that describes the generation parameters
     * @param iter  The current iteration that is appended to the class name
     * @return the name of the generated class
     */
    private String generate(Class<? extends Generative> clazz, int iter) {
        assert clazz != null;

        info(clazz, "Generating class");

        ControlValueParser parser = new ControlValueParser(Generative.args(clazz, iter));
        GenerationController controller = parser.parse();

        final String className = controller.getFileName();

        RandomCodeGenerator randomCodeGenerator = new RandomCodeGenerator(className, controller);
        randomCodeGenerator.generate();
        randomCodeGenerator.writeFile(workingDirectory.toString());

        return className;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> iterator() {

        final int totalGenerations = iterations * CLASS_TYPES.size();
        final int nClasses = CLASS_TYPES.size();

        /*
            create a new iterator that (repeatedly) iterates over the given
            class types
        */
        return new Iterator<String>() {

            /**
             * The counter that increases for each generation.
             * In order to reiterate the class array, the relative index
             * therefore has to be calculated.
             */
            private int counter = 0;

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                return counter < totalGenerations;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String next() {
                int currIteration = counter / nClasses;
                int currClassType = counter % nClasses;
                counter++;

                // get the current class type
                Class<? extends Generative> classType =
                        CLASS_TYPES.get(currClassType);

                info(classType, "%d", currIteration);

                // generate the class
                return generate(classType, currIteration);
            }
        };
    }

    // endregion
    //-------------------------------------------------------------------------
}
