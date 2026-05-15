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

class TestUseStringFormatted extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_STRING_FORMATTED);

    void assertUseFormatString(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "use-string-formatted",
                    Map.of("formatted", suggestion)
                )
            ),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testComplexFormat() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final String INITIAL_FORMAT = "%s %";
                    private static final String INDEX_FORMAT = "d: %";
                    private static final int indexWidth = 2;
                    private static final int maxInstructionWidth = 4;
                    private static final int maxArgAWidth = 6;
                    private static final String SECOND_FORMAT = "d: %";
                    private static final int maxArgBWidth = 6;
                    private static final String THIRD_FORMAT = "d: %s";

                    public static void validateNumber(String a, int index, int second, String third) {
                        String format = String.format(INITIAL_FORMAT
                                + (indexWidth)
                                + INDEX_FORMAT
                                + (maxInstructionWidth)
                                + (maxArgAWidth + 1)
                                + SECOND_FORMAT
                                + (maxArgBWidth)
                                + THIRD_FORMAT,
                            a,
                            index,
                            second,
                            third
                        );
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


