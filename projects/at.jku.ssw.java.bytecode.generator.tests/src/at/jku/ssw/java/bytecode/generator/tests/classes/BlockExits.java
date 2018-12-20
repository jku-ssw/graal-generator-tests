package at.jku.ssw.java.bytecode.generator.tests.classes;

@Generative.CLIArguments(iterations = 50, args = {"-while", "50", "-for", "50", "-dowhile", "50", "-break", "30", "-return", "30"})
public class BlockExits extends Generative {
}
