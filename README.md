# Compiler Test Suites

These test suites use the [*Java Bytecode Generator*](https://github.com/jku-ssw/java-bytecode-generator) to verify the *GraalVM*.

It uses the [mx](https://github.com/graalvm/mx) build tool to load dependencies.
To build them, it is sufficient to call

```
mx build
```

followed by 

```
mx cgen
```

to run the test suite.
