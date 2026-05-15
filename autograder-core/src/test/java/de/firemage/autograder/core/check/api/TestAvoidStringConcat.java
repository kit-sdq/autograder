package de.firemage.autograder.core.check.api;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestAvoidStringConcat  extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.AVOID_STRING_CONCAT);

    private void assertEqualsConcat(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of("suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testConcat() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static String join(String left, String right) {
                        return left.concat(right);
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsConcat(problems.next(), "left + right");

        problems.assertExhausted();
    }

    @Test
    void testConcatStream() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                public class Test {
                    public static String join(List<String> values, String right) {
                        return values.stream().map(right::concat).reduce(String::concat).orElse("");
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


