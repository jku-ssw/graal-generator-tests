# Compiler Test Suites

These test suites use the [*Java Bytecode Generator*](https://github.com/jku-ssw/java-bytecode-generator) to verify the *GraalVM*.

It uses the [mx](https://github.com/graalvm/mx) build tool to load dependencies.
To build them, it is sufficient to call

```
mx build
```

followed by 

```
mx graal_generator_tests
```

to run the test suite.

## Usage

The tool may be executed from the command line and accepts the following 
parameters:
```
mx graal_generator_tests [-h] [-o <arg>] [-r <arg>] [<class-file-name>]...
 -h,--help                           Shows the command line overview
 -o,--optimization-threshold <arg>   The number of times a class should be
                                     run before compiling it with
                                     optimization information
 -r,--repetitions <arg>              The number of times each class
                                     template should be generated
```

## Custom class files

If a bug is found and the corresponding class file is reduced, it is possible 
to re-run the test suite on an existing class file by specifying the class 
(file) names:
```
mx graal_generator_tests A.class
mx graal_generator_tests A
mx graal_generator_tests A B C.class
```

Note that such a run does not produce any class files but merely validates the 
given class files.

## Results / output

By default, the class files are saved in the relative `generated_classes` 
directory and named after their corresponding generation template 
(e.g. `HighBranchingFactor.class`) including a generation postfix denoting
the n-th repetition that generated this class.

The results of individual runs are *NOT* printed unless they describe a 
mismatch between interpretation and compilation.
