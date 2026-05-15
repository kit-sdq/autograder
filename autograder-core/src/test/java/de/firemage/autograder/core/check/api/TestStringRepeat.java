package de.firemage.autograder.core.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStringRepeat extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMMON_REIMPLEMENTATION_STRING_REPEAT);

    private void assertReimplementation(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of("suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSimpleStringRepeat() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static String repeat(String s, int n) {
                        String result = "";

                        for (int i = 0; i < n; i++) {
                            result += s;
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "result += s.repeat(n)");
        problems.assertExhausted();
    }

    @Test
    void testStringRepeatWithCustomStart() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static String repeat(String s, int n, int start) {
                        String result = "";

                        for (int i = start; i <= n; i++) {
                            result += s;
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "result += s.repeat(n + 1 - start)");
        problems.assertExhausted();
    }
}


