package de.firemage.autograder.core.check.api;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestIsLetterOrDigit extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.IS_LETTER_OR_DIGIT);

    private void assertEqualsLetterOrDigit(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of("suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = ';',
        useHeadersInDisplayName = true,
        value = {
            " Expression                                       ; Expected    ",
            " Character.isLetter(a) || Character.isDigit(a)    ; Character.isLetterOrDigit(a)  ",
            " !(Character.isLetter(a) || Character.isDigit(a)) ; Character.isLetterOrDigit(a)  ",
            " !(Character.isLetter(a) || Character.isDigit(b)) ;                               ",
            " !Character.isLetter(a) || !Character.isDigit(b)  ;                               ",
            " Character.isLetter(a) || Character.isDigit(b)    ;                               ",
            " Character.isLetter(a) || Character.isLetter(a)   ;                               ",
            " Character.isDigit(a) || Character.isDigit(a)     ;                               ",
            " Character.isLetter(a) && Character.isDigit(a)    ;                               ",
            " !Character.isLetter(a) && !Character.isDigit(a)  ; !Character.isLetterOrDigit(a) ",
        }
    )
    void testExpressions(String expression, String expected) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static boolean test(char a, char b, char c) {
                        return %s;
                    }
                }
                """.formatted(expression)
        ), PROBLEM_TYPES);

        if (expected != null) {
            assertEqualsLetterOrDigit(problems.next(), expected);
        }

        problems.assertExhausted();
    }
}


