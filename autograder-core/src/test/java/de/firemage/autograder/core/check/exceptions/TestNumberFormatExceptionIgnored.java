package de.firemage.autograder.core.check.exceptions;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestNumberFormatExceptionIgnored extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.NUMBER_FORMAT_EXCEPTION_IGNORED);

    void assertEqualsIgnored(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "number-format-exception-ignored"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testIgnoresException() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void main(String[] args) {
                        int number = Integer.parseInt("123");
                    }
                    
                    private void catcher() {
                        try {
                            Integer.parseInt("123");
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsIgnored(problems.next());

        problems.assertExhausted();
    }

    @Test
    void testHandlesException() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void main(String[] args) {
                        try {
                            Integer.parseInt("123");
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }

                    private void catcher() {
                        try {
                            Integer.parseInt("123");
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testNoExceptionHandling() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void main(String[] args) {
                        int number = Integer.parseInt("123");
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

}


