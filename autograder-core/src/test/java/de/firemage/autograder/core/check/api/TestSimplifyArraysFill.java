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

class TestSimplifyArraysFill extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.SIMPLIFY_ARRAYS_FILL);

    private void assertReimplementation(Problem problem, String suggestion) {
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
    void testSimple() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Arrays;

                public class Test {
                    public static void main(String[] args) {
                        Arrays.fill(args, 0, args.length, null); //# not ok
                        Arrays.fill(args, 1, args.length, null);
                        Arrays.fill(args, 0, args.length - 1, null);
                        Arrays.fill(args, "");
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "Arrays.fill(args, null)");

        problems.assertExhausted();
    }
}


