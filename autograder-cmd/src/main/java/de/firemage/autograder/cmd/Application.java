package de.firemage.autograder.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.firemage.autograder.api.CheckConfiguration;
import de.firemage.autograder.api.AbstractCodePosition;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.AbstractLinter;
import de.firemage.autograder.api.LinterConfigurationException;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.api.AbstractProblem;
import de.firemage.autograder.api.AbstractTempLocation;
import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.api.loader.AutograderLoader;
import de.firemage.autograder.cmd.output.Annotation;
import de.firemage.autograder.core.integrated.CoreUtil;
import de.firemage.autograder.span.Formatter;
import de.firemage.autograder.span.Highlight;
import de.firemage.autograder.span.Position;
import de.firemage.autograder.span.Span;
import de.firemage.autograder.span.Style;
import de.firemage.autograder.span.Text;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Command(mixinStandardHelpOptions = true, version = "codelinter-cmd 1.0",
        description = "Static code analysis for student java code")
public class Application implements Callable<Integer> {
    private static final int IO_EXIT_CODE = 3;
    private static final int MISC_EXIT_CODE = 10;

    @Parameters(index = "0", description = "The check configuration.")
    private String checkConfig;

    @Parameters(index = "1", description = "The root folder which contains the files to check.")
    private Path file;

    @Option(names = {"-j", "--java", "--java-version"}, defaultValue = "21", description = "Set the Java version.")
    private String javaVersion;

    @Option(names = {
            "--output-json"}, description = "Output the found problems in JSON format instead of more readable plain text")
    private boolean outputJson;

    // TODO: remove this
    @Option(names = {
            "--static-only"}, description = "Only kept here so the grading tool keeps working, does nothing.")
    private boolean staticOnly;

    @Option(names = {
            "--pass-config"}, description = "Interpret the first parameter not as the path to a config file, but as the contents of the config file")
    private boolean passConfig;

    @Option(names = {"-p", "--output-pretty"}, description = "Pretty print the output", defaultValue = "false")
    private boolean isPrettyOutput;

    @Option(names = {"--max-problems"}, description = "The maximum number of problems to report per check", defaultValue = "10")
    private int maxProblemsPerCheck;

    @Option(names = {"--debug"}, description = "Enables debug mode, note that this slows down execution", defaultValue = "false")
    private boolean isInDebugMode;

    @Spec
    private CommandSpec spec;

    private final AbstractTempLocation tempLocation;

    public Application(AbstractTempLocation tempLocation) {
        this.tempLocation = tempLocation;
    }

    private static Charset getConsoleCharset() {
        return System.console() == null ? StandardCharsets.UTF_8 : System.console().charset();
    }

    public static void main(String... args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, getConsoleCharset()));
        int exitCode = runApplication(args);
        System.exit(exitCode);
    }

    // Useful for testing
    public static int runApplication(String... args) {
        // to automatically delete the temp location on exit
        try (var tempLocation = AutograderLoader.instantiateTempLocation(Path.of(".autograder-tmp"))) {
            return new CommandLine(new Application(tempLocation)).execute(args);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not create temp location", exception);
        }
    }

    private static Highlight highlightFromCodePosition(AbstractCodePosition codePosition, String label) {
        return new Highlight(
                new Span(
                        new Position(codePosition.startLine() - 1, codePosition.startColumn() - 1),
                        new Position(codePosition.endLine() - 1, codePosition.endColumn() - 1)
                ),
                Optional.ofNullable(label),
                Style.ERROR
        );
    }

    private void execute(
            AbstractLinter linter,
            CheckConfiguration checkConfiguration,
            Consumer<Translatable> statusConsumer
    ) throws LinterException, IOException {
        if (outputJson) {
            var problems = linter.checkFile(this.file, JavaVersion.fromString(this.javaVersion), checkConfiguration, statusConsumer);
            System.out.println(">> Problems <<");
            printProblemsAsJson(problems, linter);
            return;
        }

        if (isPrettyOutput) {
            CmdUtil.beginSection("Checks");
            ProgressAnimation progress = new ProgressAnimation("Checking...");
            progress.start();
            var problems = linter.checkFile(this.file, JavaVersion.fromString(this.javaVersion), checkConfiguration, statusConsumer);
            progress.finish("Completed checks");

            if (problems.isEmpty()) {
                CmdUtil.println("No problems found - good job!");
            } else {
                CmdUtil.println("Found " + problems.size() + " problem(s):");
                problems.stream()
                        .map(problem -> {
                            AbstractCodePosition position = problem.getPosition();
                            Text sourceText = Text.fromString(0, position.readSourceFile());
                            Formatter formatter = new Formatter(
                                    System.lineSeparator(),
                                    highlightFromCodePosition(position, linter.translateMessage(problem.getExplanation()))
                            );

                            String result = "[%s]: %s - Found problem in '%s'%n".formatted(
                                    problem.getType(),
                                    problem.getCheckName(),
                                    position.toString()
                            );
                            result += formatter.render(sourceText);

                            return result;
                        })
                        .forEach(string -> CmdUtil.println(string + System.lineSeparator()));
            }

            CmdUtil.endSection();
            return;
        }

        CmdUtil.beginSection("Checks");
        ProgressAnimation progress = new ProgressAnimation("Checking...");
        progress.start();
        var problems = linter.checkFile(this.file, JavaVersion.fromString(this.javaVersion), checkConfiguration, statusConsumer);
        progress.finish("Completed checks");

        printProblems(problems, linter);

        CmdUtil.endSection();
    }

    @Override
    public Integer call() {
        if (!JavaVersion.isValidJavaVersion(javaVersion)) {
            throw new ParameterException(this.spec.commandLine(), "Unknown java version '" + javaVersion + "'");
        }

        // Depending on the structure of the project, the code might be in a subdirectory.
        // By default, we support explicitly specifying the folder to the first package (./src/main/java)
        //
        // Here we check if the project has a folder `src/<here the first package>` or `assignment/src/<here the first package>`
        // and if so, we assume that the code is in that folder.
        if (Files.exists(this.file.resolve("src"))) {
            this.file = this.file.resolve("src");
        }

        if (Files.exists(this.file.resolve("assignment/src"))) {
            this.file = this.file.resolve("assignment/src");
        }

        if (this.isInDebugMode) {
            CoreUtil.setDebugMode();
        }

        if (!outputJson) {
            System.out.println("Student source code directory is " + file);
        }

        // Create the check configuration
        CheckConfiguration checkConfiguration;
        try {
            if (passConfig) {
                checkConfiguration = CheckConfiguration.fromConfigString(checkConfig);
            } else {
                checkConfiguration = CheckConfiguration.fromConfigFile(Path.of(checkConfig));
            }
        } catch (IOException | LinterConfigurationException e) {
            e.printStackTrace();
            return IO_EXIT_CODE;
        }

        AbstractLinter linter = AutograderLoader.instantiateLinter(AbstractLinter.builder(Locale.GERMANY)
                .threads(0)
                .tempLocation(this.tempLocation)
                .maxProblemsPerCheck(this.maxProblemsPerCheck));

        Consumer<Translatable> statusConsumer = status ->
                System.out.println(linter.translateMessage(status));

        if (!Files.exists(file)) {
            CmdUtil.printlnErr("The path '%s' does not exist".formatted(file));
            return IO_EXIT_CODE;
        }

        try {
            this.execute(linter, checkConfiguration, statusConsumer);
        } catch (LinterException e) {
            e.printStackTrace();
            return MISC_EXIT_CODE;
        } catch (IOException e) {
            e.printStackTrace();
            return IO_EXIT_CODE;
        }

        return 0;
    }

    private void printProblems(List<? extends AbstractProblem> problems, AbstractLinter linter) {
        if (problems.isEmpty()) {
            CmdUtil.println("No problems found - good job!");
        } else {
            CmdUtil.println("Found " + problems.size() + " problem(s):");
            problems.stream().map(p -> this.formatProblem(p, linter)).sorted().forEach(CmdUtil::println);
        }
    }

    private void printProblemsAsJson(Collection<? extends AbstractProblem> problems, AbstractLinter linter) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonOutput = mapper.writeValueAsString(problems.stream().map(problem -> {
                AbstractCodePosition position = problem.getPosition();
                return new Annotation(
                        problem.getType(),
                        linter.translateMessage(problem.getExplanation()),
                        position.path().toString().replace("\\", "/"),
                        position.startLine(),
                        position.endLine()
                );
            }).toList());
            System.out.println(jsonOutput);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
    }

    private String formatProblem(AbstractProblem problem, AbstractLinter linter) {
        return String.format("%s %s (Source: %s)",
                problem.getDisplayLocation(),
                linter.translateMessage(problem.getExplanation()),
                linter.translateMessage(problem.getLinterName()));
    }
}
