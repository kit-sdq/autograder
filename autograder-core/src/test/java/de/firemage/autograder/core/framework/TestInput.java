package de.firemage.autograder.core.framework;

import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.SourceInfo;
import de.firemage.autograder.core.file.SourcePath;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TestInput {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestInput.class);

    private static final Pattern META_COMMENT_PATTERN = Pattern.compile("/\\*#(?<tag>[^;^#]*);?(?<comment>[^;#]*);?(?<problemType>[^#]*)#\\*/");
    private static final Pattern META_SINGLE_LINE_COMMENT_PATTERN = Pattern.compile("//#(?<tag>[^;]*);?(?<comment>[^;\\r\\n]*);?(?<problemType>\\w*)\\s*$");
    private final Path path;
    private final TestConfig config;
    private final SourceInfo sourceInfo;
    private final List<ExpectedProblem> expectedProblems;

    public TestInput(Path path) {
        this.path = path;
        this.config = TestConfig.fromPath(path);
        var sources = readSources(path);

        // Parse annotations before creating the source info because parseAnnotations rewrites the sources
        this.expectedProblems = parseAnnotations(sources, path.getFileName().toString());
        this.sourceInfo = StringSourceInfo.fromSourceStrings(sources);
    }

    public String testName() {
        return "Check E2E Test: %s".formatted(this.path.getFileName());
    }

    private static Map<String, String> readSources(Path path) {
        try {
            Path codePath = path.resolve("code");

            try (var files = Files.walk(codePath)) {
                return files.filter(Files::isRegularFile)
                        .filter(file -> file.toString().endsWith(".java"))
                        .collect(Collectors.toMap(file -> codePath.relativize(file).toString().replace(".java", ""),
                                file -> {
                                    try {
                                        return Files.readString(file, StandardCharsets.UTF_8);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<ExpectedProblem> parseAnnotations(Map<String, String> sources, String testName) {
        List<ExpectedProblem> expectedProblems = new ArrayList<>();

        for (var entry : sources.entrySet()) {
            var lines = entry.getValue().split("\\R");
            String fileName = entry.getKey() + ".java";

            StringBuilder newCode = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNumber = i + 1;

                Matcher inlineMatcher = META_COMMENT_PATTERN.matcher(line);
                for (var match : inlineMatcher.results().toList()) {
                    handleMetaComment(match, testName, fileName, lineNumber).ifPresent(expectedProblems::add);
                }
                String newLine = inlineMatcher.replaceAll("");

                Matcher eolMatcher = META_SINGLE_LINE_COMMENT_PATTERN.matcher(newLine);
                for (var match : eolMatcher.results().toList()) {
                    handleMetaComment(match, testName, fileName, lineNumber).ifPresent(expectedProblems::add);
                }
                newLine = eolMatcher.replaceAll("");

                newCode.append(newLine).append("\n");
            }
            // Using entry#setValue as it is the only safe way to replace the value of a map entry
            // while iterating over the map
            entry.setValue(newCode.toString());
        }

        return expectedProblems;
    }

    private static Optional<ExpectedProblem> handleMetaComment(MatchResult match, String testName, String fileName, int line) {
        // Named groups in matchers are not supported until Java 20
        String tag = match.group(1);
        String comment = match.group(2);
        String problemTypeString = match.group(3);

        ProblemType problemType = null;
        if (problemTypeString != null && !problemTypeString.isBlank()) {
            problemType = ProblemType.valueOf(problemTypeString);
        }

        boolean valid = switch (tag.trim().toLowerCase()) {
            case "not ok" -> true;
            case "false positive" -> {
                LOGGER.warn("False positive annotation in {}@{}:{}; reason: {}", testName, fileName, line, comment);
                yield true;
            }
            case "false negative" -> {
                LOGGER.warn("False negative annotation in {}@{}:{}; reason: {}", testName, fileName, line, comment);
                yield false;
            }
            case "", "ok" -> false;
            default ->
                    throw new RuntimeException("Unknown tag meta comment tag '%s' in test %s %s:%d".formatted(
                            tag,
                            testName,
                            fileName,
                            line)
                    );
        };

        if (valid) {
            return Optional.of(new ExpectedProblem(SourcePath.of(fileName), line, problemType, comment));
        } else {
            return Optional.empty();
        }
    }

    public Path path() {
        return this.path;
    }

    public SourceInfo sourceInfo() {
        return this.sourceInfo;
    }

    public List<ExpectedProblem> expectedProblems() {
        return this.expectedProblems;
    }

    public TestConfig config() {
        return this.config;
    }
}
