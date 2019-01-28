package at.jku.ssw.java.bytecode.generator.tests.generation.classes;

@Generative.CLIArguments(iterations = 50, args = {"-mli", "20", "-while", "50", "-for", "50", "-dowhile", "50"})
public class ManyLoops extends Generative {
}
