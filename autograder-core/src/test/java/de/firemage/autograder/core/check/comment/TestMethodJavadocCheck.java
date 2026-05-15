package de.firemage.autograder.core.check.comment;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.SourceInfo;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestMethodJavadocCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.JAVADOC_MISSING_PARAMETER_TAG,
        ProblemType.JAVADOC_UNKNOWN_PARAMETER_TAG,
        ProblemType.JAVADOC_UNEXPECTED_TAG
    );

    void assertEqualsMissing(Problem problem, String param) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "javadoc-method-exp-param-missing",
                Map.of("param", param)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsUnknownParam(Problem problem, String param) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "javadoc-method-exp-param-unknown",
                Map.of("param", param)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsUnknownTag(Problem problem, String tag) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "javadoc-unexpected-tag",
                Map.of("tag", tag)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    private static String makeJavadoc(String... entries) {
        return "/**%s%n */".formatted(
            Arrays.stream(entries).map("\n * "::concat).collect(Collectors.joining())
        );
    }

    private static SourceInfo makeTestClass(String returnType, String parameter, String... javadocEntries) {
        return StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    %s
                    public %s method(%s) {}
                }
                """.formatted(makeJavadoc(javadocEntries), returnType, parameter)
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Parameter     | Tags                  | Expected          ",
            "               |                       |                   ",
            " int a, int b  | @param a, @param b    |                   ",
            " int a, int b  | @param a              | b                 ",
            " int a, int b  |                       | a, b              ",
        }
    )
    void testMissingParameterTag(String parameter, String tags, String expected) throws IOException, LinterException {
        if (parameter == null) {
            parameter = "";
        }
        if (tags == null) {
            tags = "";
        }
        if (expected == null) {
            expected = "";
        }

        ProblemIterator problems = this.checkIterator(
            makeTestClass("void", parameter, tags.split(", ")),
            PROBLEM_TYPES
        );

        for (String missingParam : expected.split(", ")) {
            if (missingParam.isBlank()) {
                continue;
            }
            assertEqualsMissing(problems.next(), missingParam);
        }

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Parameter     | Tags                              | Expected  ",
            " int a         | @param a                          |           ",
            " int a         | @param a, @param b                | b         ",
            " int a         | @param a, @param b, @param c      | b, c      ",
        }
    )
    void testUnknownParameterTag(String parameter, String tags, String expected) throws IOException, LinterException {
        if (parameter == null) {
            parameter = "";
        }
        if (tags == null) {
            tags = "";
        }
        if (expected == null) {
            expected = "";
        }

        ProblemIterator problems = this.checkIterator(
            makeTestClass("void", parameter, tags.split(", ")),
            PROBLEM_TYPES
        );

        for (String missingParam : expected.split(", ")) {
            if (missingParam.isBlank()) {
                continue;
            }
            assertEqualsUnknownParam(problems.next(), missingParam);
        }

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Parameter     | Tags          | Expected  ",
            // all valid tags:
            " int a         | @param a      |           ",
            "               | @return       |           ",
            "               | @throws       |           ",
            "               | @exception    |           ",
            "               | @see          |           ",
            "               | @unknown      |           ",
            "               | @deprecated   |           ",
            // invalid tags:
            "               | @version      | version   ",
            "               | @author       | author    ",
            "               | @since        | since     ",
        }
    )
    void testUnknownTag(String parameter, String tags, String expected) throws IOException, LinterException {
        if (parameter == null) {
            parameter = "";
        }
        if (tags == null) {
            tags = "";
        }
        if (expected == null) {
            expected = "";
        }

        ProblemIterator problems = this.checkIterator(
            makeTestClass("void", parameter, tags.split(", ")),
            PROBLEM_TYPES
        );

        for (String missingParam : expected.split(", ")) {
            if (missingParam.isBlank()) {
                continue;
            }
            assertEqualsUnknownTag(problems.next(), missingParam);
        }

        problems.assertExhausted();
    }
}


