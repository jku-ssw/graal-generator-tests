
import mx
from mx_compiler import run_java

_suite = mx.suite('graal-generator-tests')

def compile_generated(args=None):
    """verify compiler using generated classes"""
    vmargs = mx.get_runtime_jvm_args(['CGEN'])

    run_java(vmargs + ['-XX:-UseJVMCIClassLoader'] + ["at.jku.ssw.java.bytecode.generator.tests.CompileGeneratedClasses"] + args)

mx.update_commands(_suite, {
    'cgen' : [compile_generated, '[args...]']
})
