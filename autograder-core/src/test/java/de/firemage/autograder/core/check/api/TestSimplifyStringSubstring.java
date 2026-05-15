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

class TestSimplifyStringSubstring extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.SIMPLIFY_STRING_SUBSTRING);

    private void assertSubstring(Problem problem, String target, String start) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of("suggestion", "%s.substring(%s)".formatted(target, start))
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSubstring() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        String example = "example";
                        
                        System.out.println(example.substring(1, example.length()));
                        System.out.println(example.substring(3, example.length()));
                        System.out.println(example.substring(args.length, example.length()));
                        System.out.println(example.substring(0, 7));
                        System.out.println(example.substring(0, example.length() - 1));
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertSubstring(problems.next(), "example", "1");
        assertSubstring(problems.next(), "example", "3");
        assertSubstring(problems.next(), "example", "args.length");

        problems.assertExhausted();
    }
}


