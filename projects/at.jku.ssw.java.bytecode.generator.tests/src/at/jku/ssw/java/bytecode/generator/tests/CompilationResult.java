package at.jku.ssw.java.bytecode.generator.tests;

import jdk.vm.ci.code.InstalledCode;

import java.util.List;
import java.util.Optional;

class CompilationResult {
    private final Class<?> clazz;
    private final InstalledCode main;
    private final List<Optional<InstalledCode>> others;
    private boolean invalidated;

    CompilationResult(Class<?> clazz, InstalledCode main, List<Optional<InstalledCode>> others) {
        assert clazz != null;
        assert main != null;
        assert others != null;

        this.clazz = clazz;
        this.main = main;
        this.others = others;
        this.invalidated = false;
    }

    void invalidate() {
        if (invalidated)
            return;

        main.invalidate();
        others.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(InstalledCode::invalidate);
        invalidated = true;
    }

    Class<?> getClazz() {
        return clazz;
    }

    InstalledCode getMain() {
        return main;
    }
}
