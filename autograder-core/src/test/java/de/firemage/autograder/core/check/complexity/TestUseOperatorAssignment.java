package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseOperatorAssignment extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_OPERATOR_ASSIGNMENT);

    private void assertUseOperatorAssignment(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of("suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression          | Arguments             | Expected     ",
            " a = a + b           | int a, int b          | a += b       ",
            " a = a - b           | int a, int b          | a -= b       ",
            " a = a * b           | int a, int b          | a *= b       ",
            " a = a / b           | int a, int b          | a /= b       ",
            " a = a % b           | int a, int b          | a %= b       ",
            " a = b + a           | int a, int b          | a += b       ",
            " a = b - a           | int a, int b          |              ",
            " a = b * a           | int a, int b          | a *= b       ",
            " a = b / a           | int a, int b          |              ",
            " a = b % a           | int a, int b          |              ",
            " arr[0] = arr[0] + 1 | int[] arr             | arr[0] += 1  ",
            " arr[0] = arr[1] + 1 | int[] arr             |              ",
            " a = a - b + c       | int a, int b, int c   |              ",
            " a = b - c + a       | int a, int b, int c   | a += (b - c) ",
            " a = (b + c) * a     | int a, int b, int c   | a *= (b + c) ",
            " a = b + c * a       | int a, int b, int c   |              ",
            " a = b - a + c       | int a, int b, int c   |              ",
            " a = a + b + c - b   | int a, int b, int c   |              ",
            " s = s + \" \"       | String s              | s += \" \"   ",
            " s = \" \" + s       | String s              |              ",
            // TODO: this one is not recognized:
            //" s = s + s + s       | String s              | s += s + s   ",
        }
    )
    void testDifferentTypes(String expression, String arguments, String expected) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            "public class Test { public void foo(%s) { %s; } }".formatted(
                arguments,
                expression
            )
        ), PROBLEM_TYPES);

        if (expected != null) {
            assertUseOperatorAssignment(problems.next(), expected);
        }

        problems.assertExhausted();
    }
}
