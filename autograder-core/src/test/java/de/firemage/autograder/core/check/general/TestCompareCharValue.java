package de.firemage.autograder.core.check.general;

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

class TestCompareCharValue extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "compare-char-value";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMPARE_CHAR_VALUE);

    private void assertEqualsCompare(Problem problem, String expression, int intValue, char charValue) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "expression", expression,
                        "intValue", intValue,
                        "charValue", charValue
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testEqualsCompare() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        char c = 'a';
                        if (c == 97) {
                            System.out.println("a");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsCompare(problems.next(), "c", 97, 'a');

        problems.assertExhausted();
    }

    @Test
    void testChainedComparison() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        char symbol = 'a';
                        boolean isValid = (symbol > 47 && symbol < 58) || symbol == '*' || symbol == '-' || symbol == '+';
                        isValid = (symbol > '/' && symbol < ':') || symbol == '*' || symbol == '-' || symbol == '+';
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsCompare(problems.next(), "symbol", 47, '/');
        assertEqualsCompare(problems.next(), "symbol", 58, ':');

        problems.assertExhausted();
    }

    @Test
    void testCompareWithZero() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        char symbol = 'a';
                        boolean isValid = symbol != 0;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testCompareWithIntConstant() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final int EXPECTED_VALUE = 47;

                    public static void main(String[] args) {
                        char symbol = 'a';
                        boolean isValid = symbol == EXPECTED_VALUE;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsCompare(problems.next(), "symbol", 47, '/');

        problems.assertExhausted();
    }

    @Test
    void testCompareWithBoxedInt() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final Integer EXPECTED_VALUE = 47;

                    public static void main(String[] args) {
                        char symbol = 'a';
                        boolean isValid = symbol == EXPECTED_VALUE;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsCompare(problems.next(), "symbol", 47, '/');

        problems.assertExhausted();
    }


    @Test
    void testCompareWithComplexCharExpression() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        boolean isValid = args[0].charAt(args.length) == 47;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsCompare(problems.next(), "args[0].charAt(args.length)", 47, '/');

        problems.assertExhausted();
    }

    @Test
    void testCompareTooLargeInt() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        char symbol = 'a';
                        boolean isValid = symbol <= 1000;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


