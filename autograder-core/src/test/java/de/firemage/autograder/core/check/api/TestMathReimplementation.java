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

class TestMathReimplementation extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.COMMON_REIMPLEMENTATION_SQRT,
        ProblemType.COMMON_REIMPLEMENTATION_HYPOT,
        ProblemType.COMMON_REIMPLEMENTATION_MAX_MIN
    );

    private void assertEqualsReimplementation(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "suggestion", suggestion
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSqrt() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private int sqrt(int x) {
                        return (int) Math.pow(x, 0.5);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "Math.sqrt(x)");

        problems.assertExhausted();
    }

    @Test
    void testHypot() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private double exampleA(int x, int y) {
                        return Math.sqrt(x * x + y * y);
                    }

                    private double exampleB(int x, int y) {
                        return Math.sqrt(Math.pow(x, 2) + y * y);
                    }

                    private double exampleC(int x, int y) {
                        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                    }
                    
                    private double exampleD(int x, int y) {
                        return Math.pow(Math.pow(x, 2) + Math.pow(y, 2), 0.5);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "Math.hypot(x, y)");
        assertEqualsReimplementation(problems.next(), "Math.hypot(x, y)");
        assertEqualsReimplementation(problems.next(), "Math.hypot(x, y)");
        assertEqualsReimplementation(problems.next(), "Math.hypot(x, y)");

        problems.assertExhausted();
    }


    @Test
    void testMax() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void foo(int a, int b) {
                        int left = a;
                        int right = b;
                        
                        if (left < right) {
                            left = right;
                        }

                        if (left <= right) {
                            left = right;
                        }

                        if (right > left) {
                            left = right;
                        }

                        if (right >= left) {
                            left = right;
                        }

                        if (0 >= left) {
                            left = 0;
                        }
                        
                        if (1 > left) {
                            left = 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);


        List<String> expectedProblems = List.of(
            "left = Math.max(left, right)",
            "left = Math.max(left, right)",
            "left = Math.max(left, right)",
            "left = Math.max(left, right)",
            "left = Math.max(left, 0)",
            "left = Math.max(left, 1)"
        );

        for (String expectedProblem : expectedProblems) {
            assertEqualsReimplementation(problems.next(), expectedProblem);
        }

        problems.assertExhausted();
    }

    @Test
    void testMaxChangedAssignment() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.List;

                public class Main {
                    public static void foo(int longestTrip, List<Integer> trip) {
                        if (longestTrip < trip.size()) {
                            longestTrip = trip.size() - 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMin() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void foo(int a, int b) {
                        int left = a;
                        int right = b;
                        
                        if (right < left) {
                            left = right;
                        }

                        if (right <= left) {
                            left = right;
                        }

                        if (left > right) {
                            left = right;
                        }

                        if (left >= right) {
                            left = right;
                        }

                        if (left >= 0) {
                            left = 0;
                        }
                        
                        if (left > 1) {
                            left = 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);


        List<String> expectedProblems = List.of(
            "left = Math.min(left, right)",
            "left = Math.min(left, right)",
            "left = Math.min(left, right)",
            "left = Math.min(left, right)",
            "left = Math.min(left, 0)",
            "left = Math.min(left, 1)"
        );

        for (String expectedProblem : expectedProblems) {
            assertEqualsReimplementation(problems.next(), expectedProblem);
        }

        problems.assertExhausted();
    }


    @Test
    void testMinMaxWithElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void foo(int a, int b) {
                        int result = 0;

                        if (a < b) {
                            result = a;
                        } else {
                            result = b;
                        }

                        if (a <= b) {
                            result = a;
                        } else {
                            result = b;
                        }

                        if (a < b) {
                            result = b;
                        } else {
                            result = a;
                        }

                        if (a <= b) {
                            result = b;
                        } else {
                            result = a;
                        }
                        
                    }
                }
                """
        ), PROBLEM_TYPES);

        List<String> expectedProblems = List.of(
            "result = Math.min(b, a)",
            "result = Math.min(b, a)",
            "result = Math.max(a, b)",
            "result = Math.max(a, b)"
        );

        for (String expectedProblem : expectedProblems) {
            assertEqualsReimplementation(problems.next(), expectedProblem);
        }

        problems.assertExhausted();
    }
}


