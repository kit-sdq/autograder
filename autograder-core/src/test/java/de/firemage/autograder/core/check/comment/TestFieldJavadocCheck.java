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

class TestFieldJavadocCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.JAVADOC_UNEXPECTED_TAG);

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

    private static SourceInfo makeTestClass(String... javadocEntries) {
        return StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    %s
                    public String string;
                    %s
                    private String b;
                    // ^ private fields should be ignored
                }
                """.formatted(makeJavadoc(javadocEntries), makeJavadoc(javadocEntries))
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Tags          | Expected          ",
            " @see          |                   ",
            " @unknown      |                   ",
            " @deprecated   |                   ",
            "               |                   ",
            " @param        | param             ",
            " @return       | return            ",
            " @throws       | throws            ",
            " @exception    | exception         ",
            " @version      | version           ",
            " @author       | author            ",
            " @since        | since             ",
        }
    )
    void testMissingParameterTag(String tags, String expected) throws IOException, LinterException {
        if (tags == null) {
            tags = "";
        }
        if (expected == null) {
            expected = "";
        }

        ProblemIterator problems = this.checkIterator(
            makeTestClass(tags.split(", ")),
            PROBLEM_TYPES
        );

        for (String value : expected.split(", ")) {
            if (value.isBlank()) {
                continue;
            }
            assertEqualsUnknownTag(problems.next(), value);
        }

        problems.assertExhausted();
    }
}


