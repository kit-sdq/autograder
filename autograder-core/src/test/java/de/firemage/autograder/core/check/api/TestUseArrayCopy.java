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

class TestUseArrayCopy extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMMON_REIMPLEMENTATION_ARRAY_COPY);

    private void assertEqualsReimplementation(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of(
                        "suggestion", suggestion
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSimpleArrayCopy() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static int[] copyArray(int[] toCopy) {
                        int[] result = new int[toCopy.length];

                        for (int i = 0; i < toCopy.length; i++) {
                            result[i] = toCopy[i];
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "System.arraycopy(toCopy, 0, result, 0, toCopy.length)");
        problems.assertExhausted();
    }

    @Test
    void testOperatorAssignment() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static int[] copyArray(int[] toCopy) {
                        int[] result = new int[toCopy.length];

                        for (int i = 0; i < toCopy.length; i++) {
                            result[i] += toCopy[i];
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testArrayCopyWithCustomStart() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static int[] copyArray(int start, int[] toCopy) {
                        int[] result = new int[toCopy.length];

                        for (int i = start; i <= toCopy.length - 1; i++) {
                            result[i] = toCopy[i];
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(
            problems.next(),
            "System.arraycopy(toCopy, start, result, start, toCopy.length - start)"
        );
        problems.assertExhausted();
    }

    @Test
    void testDoubleArrayCopy() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "MatrixUtils",
            """
                public class MatrixUtils {
                    public static int[][] copyMatrix(int[][] matrix) {
                        int n = matrix.length;
                        int m = matrix[0].length;

                        int[][] result = new int[n][m];
                        for (int i = 0; i < n; i++) {
                            for (int j = 0; j < m; j++) { // Not Ok (= System.arraycopy(matrix[i], 0, result[i], 0, m))
                                result[i][j] = matrix[i][j];
                            }
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "System.arraycopy(matrix[i], 0, result[i], 0, m)");
        problems.assertExhausted();
    }
}


