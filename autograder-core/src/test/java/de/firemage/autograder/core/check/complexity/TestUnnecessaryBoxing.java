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

class TestUnnecessaryBoxing extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.UNNECESSARY_BOXING);

    void assertUnnecessary(Problem problem, String original, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "suggest-replacement",
                Map.of(
                    "original", original,
                    "suggestion", suggestion
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testBoxedVariableNeverAssigned() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;

            class Test {
                Integer integer; //# ok
            
                public static void main(String[] args) {
                    Double uninitialized; //# ok
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testBoxedVariableOnlyInitialized() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;

            class Test {
                Integer integer = 1; //# not ok
            
                public static void main(String[] args) {
                    Double number = 2.0d; //# not ok
                }
            }
            """
        ), PROBLEM_TYPES);

        assertUnnecessary(problems.next(), "Integer", "int");
        assertUnnecessary(problems.next(), "Double", "double");

        problems.assertExhausted();
    }

    @Test
    void testBoxedVariableExplicitlyAssignedNull() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                Float price = null; //# ok
                Double value = 1.0d;

                void foo() {
                    value = null; //# ok
                    
                    Integer i = 1;
                    
                    i = null;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testGenericContainer() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;

            class Test {
                private List<Double> list = List.of();
                private Integer[] array = new Integer[10];
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testBoxedReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                Double potentiallyNull = getDouble(true); /*# ok #*/

                private static Double getDouble(boolean condition) {
                    if (condition) {
                        return null;
                    } else {
                        return 1.123d;
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


