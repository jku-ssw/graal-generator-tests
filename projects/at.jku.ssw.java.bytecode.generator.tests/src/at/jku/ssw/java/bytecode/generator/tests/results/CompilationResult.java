package at.jku.ssw.java.bytecode.generator.tests.results;

import jdk.vm.ci.code.InstalledCode;

import java.util.List;
import java.util.Optional;

public class CompilationResult {
    private final Class<?> clazz;
    private final InstalledCode main;
    private final List<Optional<InstalledCode>> others;

    public CompilationResult(Class<?> clazz,
                             InstalledCode main,
                             List<Optional<InstalledCode>> others) {
        assert clazz != null;
        assert main != null;
        assert others != null;

        this.clazz = clazz;
        this.main = main;
        this.others = others;
    }

    public Class<?> clazz() {
        return clazz;
    }

    public InstalledCode main() {
        return main;
    }

    public List<Optional<InstalledCode>> others() {
        return others;
    }
}
