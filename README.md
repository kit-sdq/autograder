## Automatically run static & dynamic analysis on student code to aid grading.

### An incomplete list of currently implemented checks can be found [here](checks.md)

### Usage

Autograder requires at least Java 17.
The main class of the command line application is [de.firemage.autograder.cmd.Application](autograder-cmd/src/main/java/de/firemage/autograder/cmd/Application.java).

Configuration is done using command line flags and a check config file.
The command line parameters are:
- Path #1: The path to the check config file
- Path #2: The path to the student's code directory. It should contain the root package structure (e.g., `src/main/java`). Non-Java files are ignored.
- Java version: Set with `-j <int>` or `--java <int>`. Default: `25`

**Dynamic/dynamic analysis is not supported in this version.** The `-s`/`--static` flags remain for compatibility but are no-ops.

Example:
```bash
java -jar autograder-cmd.jar config.yml /path/to/submission/src/main/java
```
* `-s` / `--static`: Disable dynamic analysis.

The check config file must be a valid YAML file that configures the tests to run.
Autograder can be used as a library by just using the module `autograder-core`.

```bash
java -jar autograder-cmd.jar /path/to/config.yml /path/to/submission/src/main/java
```

### Additional information

The code is mostly analyzed through spoon, but PMD, and error-prone are supported as well.
Implemented checks can be found in the [checks directory](autograder-core/src/main/java/de/firemage/autograder/core/check).

The [sample_config.yaml](sample_config.yaml) contains all available checks that can be enabled.
If you have an idea for another check, open an issue or implement the check yourself and open a pull request.

Note: To get accurate results, ensure that the submission directory contains valid Java source code with a standard package structure (e.g., `src/main/java`).

### Development
As of now, the following tools can be used in checks:
* PMD XPath expressions to query the source code. 
* A model of the source code built with [Spoon](https://github.com/INRIA/spoon). Most non-trivial checks use this model.
* An (unstructured) list of events generated during the test runs.
* A dependency graph between classes of the source code, built with [JGraphT](https://jgrapht.org/).
* (not yet implemented) A full static dataflow analysis built with [Soot](https://github.com/soot-oss/soot) and [Heros](https://github.com/Sable/heros).

All checks except the PMD-based checks must inherit from PMD-based tests must inherit from [IntegratedCheck](autograder-core/src/main/java/de/firemage/autograder/core/integrated/IntegratedCheck.java).
PMD-based checks must inherit from [PMDCheck](autograder-core/src/main/java/de/firemage/autograder/core/pmd/PMDCheck.java).

If you change the executor or the agent, run `mvn compile` for all so that the jar files in [autograder-core resources](autograder-core) get updated.

The directory [test_submissions](test_submissions) contains sample student code you can use when developing or testing checks:
- [Bank](test_submissions/Bank) – an implementation showing separation of logic and UI; not fully submission-ready.
- [A1](test_submissions/A1) – a submission to a university final task.

There is also simple testing framework for checks implemented in the [test directory](autograder-core/src/test/java).

### Compiling the project

The project uses Maven. To compile the project, run the top-level command:
```bash
mvn package -DskipTests
```

The application jar will be at `autograder-cmd/target/autograder-cmd.jar`. Visit the [Releases page](https://github.com/kit-sdq/autograder/releases) to download pre-built artifacts.
