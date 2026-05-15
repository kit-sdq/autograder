package de.firemage.autograder.extra.check.general;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseDifferentVisibility extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.USE_DIFFERENT_VISIBILITY,
        ProblemType.USE_DIFFERENT_VISIBILITY_PEDANTIC,
        ProblemType.USE_DIFFERENT_VISIBILITY_PUBLIC_FIELD
    );

    void assertDifferentVisibility(Problem problem, String name, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "use-different-visibility",
                Map.of("name", name, "suggestion", suggestion)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testPublicConstructorAbstractClass() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                Parent parent = new Child();
                                System.out.println(parent);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Parent",
                    """
                    public abstract class Parent {
                        public Parent() {
                        }
                    }
                    """
                ),
                Map.entry(
                    "Child",
                    """
                    public class Child extends Parent {
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        Problem problem = problems.next();
        assertDifferentVisibility(problem, "Parent", "protected");
        assertEquals(ProblemType.USE_DIFFERENT_VISIBILITY_PEDANTIC, problem.getProblemType());

        problems.assertExhausted();
    }
}
