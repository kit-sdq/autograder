package de.firemage.autograder.core.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseFormatString extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "use-format-string";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_FORMAT_STRING);

    void assertUseFormatString(String expected, Problem problem) {
        assertEquals(ProblemType.USE_FORMAT_STRING, problem.getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "formatted", expected
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMotivatingExample() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final int MIN_NUMBER = 1;
                    private static final int MAX_NUMBER = 3;

                    public static void validateNumber(int number) {
                        if (number < MIN_NUMBER || number > MAX_NUMBER || number % 2 == 0) {
                            throw new IllegalArgumentException("Board must be an odd number between " + MIN_NUMBER + " and " + MAX_NUMBER);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUseFormatString(
            "\"Board must be an odd number between %d and %d\".formatted(MIN_NUMBER, MAX_NUMBER)",
            problems.next()
        );
        problems.assertExhausted();
    }

    @Test
    void testFormatStringBuilder() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        StringBuilder stringBuilder = new StringBuilder();
                        
                        stringBuilder.append("[").append(args[0]).append("]");
                        
                        System.out.println(stringBuilder.toString());
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUseFormatString("stringBuilder.append(\"[%s]\".formatted(args[0]))", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testMinimumStringConstantLiterals() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public class Field {
                    private static final String OPEN_BRACKET = "[";
                    private static final String CLOSE_BRACKET = "]";

                    private int number;

                    @Override
                    public String toString() {
                        return OPEN_BRACKET + number + CLOSE_BRACKET;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUseFormatString("\"[%d]\".formatted(number)", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testTooFewStringLiterals() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public class Field {
                    private String left;
                    private int number;
                    private String right;

                    @Override
                    public String toString() {
                        return this.left + number + this.right + ".";
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testBuildFormatString() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public class Field {
                    private String left;
                    private int number;
                    private String right;

                    @Override
                    public String toString() {
                        return String.format("output: " + "%" + this.number + "d", this.number);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testLongConstant() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public class Field {
                    private static final String MY_VERY_LONG_CONSTANT_STRING = "This is a very long constant string that is used in the code."
                        + "It is so long that it has to be split over multiple lines."
                        + "It is so long that it has to be split over multiple lines."
                        + "It is so long that it has to be split over multiple lines."
                        + "It is so long that it has to be split over multiple lines.";
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testOnlyLiterals() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public class Field {
                    private static final String SOME_CONSTANT = "This is a constant string.";
                    private String left;
                    private int number;
                    private String right;

                    @Override
                    public String toString() {
                        return "a" + "b" + SOME_CONSTANT + SOME_CONSTANT;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testOnlyVariables() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public class Field {
                    private String left;
                    private int number;
                    private String right;

                    @Override
                    public String toString() {
                        return left + number + right + number + left + right + number;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


