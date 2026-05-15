package de.firemage.autograder.extra.check.general;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestVariableShouldBeConstant extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.FIELD_SHOULD_BE_CONSTANT,
        ProblemType.LOCAL_VARIABLE_SHOULD_BE_CONSTANT
    );

    private void assertProblem(Problem problem, String variable, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "variable-should-be",
                    Map.of(
                        "variable", variable,
                        "suggestion", suggestion
                    )
                )
            ),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testDefaultVisibility() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    String exampleConstant = "example";
                }
                """
        ), PROBLEM_TYPES);

        assertProblem(problems.next(), "exampleConstant", "static final String EXAMPLE_CONSTANT = \"example\"");

        problems.assertExhausted();
    }

    @Test
    void testOtherVisibility() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private final int variable = 1;
                }
                """
        ), PROBLEM_TYPES);

        assertProblem(problems.next(), "variable", "private static final int VARIABLE = 1");

        problems.assertExhausted();
    }

    @Test
    void testLocalFinalVariableWithWrongNamingConvention() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    void foo() {
                        final int DAMAGE = 1;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertProblem(problems.next(), "DAMAGE", "private static final int DAMAGE = 1");

        problems.assertExhausted();
    }
}
