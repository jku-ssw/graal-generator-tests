package at.jku.ssw.java.bytecode.generator.tests.generation.classes;

@Generative.CLIArguments(iterations = 50, args = {"-ibf", "10", "-while", "30", "-for", "30", "-dowhile", "30"})
public class HighBranchingFactor extends Generative {
}
