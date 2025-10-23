package de.firemage.autograder.extra.errorprone;

import de.firemage.autograder.core.compiler.CompilerProvider;
import de.firemage.autograder.core.file.CompilationUnit;
import de.firemage.autograder.core.file.SourceInfo;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.AbstractTempLocation;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstracts away the compiler and ensures that error-prone is executed correctly.
 *
 * @param javaVersion the java version with which to compile
 * @param lints       the lints that should be emitted
 */
record ErrorProneCompiler(JavaVersion javaVersion, AbstractTempLocation tempLocation,
                          List<ErrorProneLint> lints) implements Serializable {
    /**
     * Compiles the given source files and returns the emitted lints.
     *
     * @param input the source code to compile
     * @return the emitted lints
     * @throws IOException if the compilation failed
     */
    List<ErrorProneDiagnostic> compile(SourceInfo input) throws IOException {
        // error-prone is a java compiler plugin that emits lints while compiling code
        // It requires access to internal APIs that have to be exported through these
        // flags.
        //
        // Normally you would have to prefix those flags with -J, so they are available
        // when error-prone is running.
        //
        // The JavaCompiler API is used to not have to manually parse the error messages.
        //
        // The problem is that the JavaCompiler does not support the -J flag and instead
        // inherits the exports from the JVM it is running in. Autograder will obviously
        // not have these flags set, so instead of requiring them (would be annoying for
        // all contributors and IDE setup), a new JVM is launched with the flags set.
        VMLauncher vmLauncher = VMLauncher.fromDefault(this.tempLocation);

        // use explicit type, so it is serializable
        ArrayList<ErrorProneDiagnostic> diagnostics;
        try {
            diagnostics = vmLauncher
                .runInNewJVM(() -> new ArrayList<>(this.internalCompile(input)))
                // wait for the compiler to finish
                .join();
        } catch (InterruptedException exception) {
            // not sure how to handle InterruptedException, so just do something and hope it never happens
            Thread.currentThread().interrupt();
            throw new IllegalStateException("unreachable");
        }

        return diagnostics;
    }

    private List<ErrorProneDiagnostic> internalCompile(SourceInfo input) throws IOException {
        List<CompilationUnit> compilationUnits = input.compilationUnits();
        // TODO: charset should be for each file individually
        Charset charset = compilationUnits.get(0).charset();

        if (compilationUnits.isEmpty()) {
            throw new IllegalArgumentException("Nothing found to compile in " + input.path());
        }

        JavaCompiler compiler = CompilerProvider.findSystemCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        StringWriter output = new StringWriter();

        boolean isSuccessful;
        try (AbstractTempLocation tempLocation = this.tempLocation.createTempDirectory("classes")) {
            isSuccessful = compiler.getTask(
                output,
                compiler.getStandardFileManager(diagnosticCollector, Locale.US, charset),
                diagnosticCollector,
                List.of(
                    "-processorpath",
                    System.getProperty("java.class.path"),
                    "-d", // write class files to a temporary directory
                    tempLocation.toPath().toString(),
                    "-XDcompilePolicy=simple",
                    "--should-stop=ifError=FLOW",
                    Stream.concat(
                            Stream.of(
                                "-Xplugin:ErrorProne",
                                "-XepDisableAllChecks"
                            ),
                            this.lints.stream().map("-Xep:%s:WARN"::formatted)
                        )
                        .collect(Collectors.joining(" "))
                ),
                null,
                compilationUnits.stream().map(CompilationUnit::toJavaFileObject).toList()
            ).call();
        }

        output.flush();
        output.close();

        if (!isSuccessful) {
            throw new IllegalArgumentException("Failed to compile %s: %s".formatted(input.path(), output));
        }

        return diagnosticCollector.getDiagnostics()
            .stream()
            // only keep error-prone diagnostics
            .filter(diagnostic -> diagnostic.getCode().equals("compiler.warn.error.prone"))
            .map(diagnostic -> ErrorProneDiagnostic.from(diagnostic, input))
            .toList();
    }
}
