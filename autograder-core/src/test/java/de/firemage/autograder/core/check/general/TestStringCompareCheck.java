package de.firemage.autograder.core.check.general;

import de.firemage.autograder.api.JavaVersion;
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

class TestStringCompareCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.STRING_COMPARE_BY_REFERENCE);

    private void assertEqualsSuggestion(Problem problem, String original, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                        "suggest-replacement",
                    Map.of("original", original, "suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testHiddenUnusedParentField() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                String a = "hello";
                                String b = "hello";
                                
                                if (a == b) {
                                    System.out.println("Equal");
                                }
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsSuggestion(problems.next(), "a == b", "a.equals(b)");

        problems.assertExhausted();
    }
}
