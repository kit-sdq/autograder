package de.firemage.autograder.extra.check.general;

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

class TestDoubleBraceInitializationCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.DOUBLE_BRACE_INITIALIZATION
    );

    void assertDoubleBraceInit(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "double-brace-init"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testDoubleBraceInit() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.HashSet;
                        import java.util.Set;

                        public class Example {
                            public static void main(String[] args) {
                                Set<String> countries = new HashSet<String>() { /*# not ok #*/
                                    {
                                        add("Germany");
                                    }
                                };
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertDoubleBraceInit(problems.next());

        problems.assertExhausted();
    }
}
