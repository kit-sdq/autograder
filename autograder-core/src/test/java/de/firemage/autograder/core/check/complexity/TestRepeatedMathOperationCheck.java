package de.firemage.autograder.core.check.complexity;

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

class TestRepeatedMathOperationCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REPEATED_MATH_OPERATION);

    void assertEqualsRepeat(String suggestion, Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "common-reimplementation",
                Map.of("suggestion", suggestion)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testRepeat() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        int a = 4;
                        int b = 3;
                        
                        int c = a + a + a + a;
                        int d = b * b * b * b * b;

                        int[] array = new int[10];
                        int[] array2 = new int[10];

                        int e = array.length * array.length * array.length * array.length * array.length;
                        int f = array2.length * array.length * array2.length;
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRepeat("a * 4", problems.next());
        assertEqualsRepeat("Math.pow(b, 5)", problems.next());
        assertEqualsRepeat("Math.pow(array.length, 5)", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testMinimumThresholdPlus() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        int[] array = new int[10];
                        int[] array2 = new int[10];

                        int a = 4;
                        int b = 3;
                        
                        int c = a + a;
                        int d = b + b + b;
                        int e = array.length + array.length + array.length + array.length;

                        int f = array2.length + array2.length + array2.length + array2.length + array2.length;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRepeat("a * 2", problems.next());
        assertEqualsRepeat("b * 3", problems.next());
        assertEqualsRepeat("array.length * 4", problems.next());
        assertEqualsRepeat("array2.length * 5", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testMinimumThresholdMul() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        int[] array = new int[10];
                        int[] array2 = new int[10];

                        int a = 4;
                        int b = 3;
                        
                        int c = a * a;
                        int d = b * b * b;
                        int e = array.length * array.length * array.length * array.length;

                        int f = array2.length * array2.length * array2.length * array2.length * array2.length;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRepeat("Math.pow(b, 3)", problems.next());
        assertEqualsRepeat("Math.pow(array.length, 4)", problems.next());
        assertEqualsRepeat("Math.pow(array2.length, 5)", problems.next());

        problems.assertExhausted();
    }


    @Test
    void testFalsePositiveFieldAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        int[] a = new int[10];
                        
                        int b = a.length + args.length;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRecursiveSuggestion() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        int index = args.length;

                        System.out.println(args[index + index + 1]);
                        int b = index * 2;
                        System.out.println(args[(index + index) + (b + b + b)]);
                    }
                }
                """
        ), PROBLEM_TYPES);
        assertEqualsRepeat("System.out.println(args[index * 2 + 1])", problems.next());
        assertEqualsRepeat("System.out.println(args[index * 2 + b * 3])", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testMulAndPlusComplexOptimization() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static int test(int a, int b, int c, int d) {
                        return a + a + (a + b - c) + 1 + d * a * a * (b * b * (a * a));
                    }
                }
                """
        ), PROBLEM_TYPES);
        assertEqualsRepeat("a * 2 + (a + b - c) + 1 + d * Math.pow(a, 4) * (b * b)", problems.next());

        problems.assertExhausted();
    }
}


