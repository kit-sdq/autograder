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

class TestRedundantAssignment extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REDUNDANT_ASSIGNMENT);

    void assertEqualsRedundant(Problem problem, String variable) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "redundant-assignment",
                Map.of("variable", variable)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMotivation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public void test() {
                        int a = 5;
                        
                        System.out.println(a);
                        
                        a = 3;
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "a");

        problems.assertExhausted();
    }

    @Test
    void testInLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public void test() {
                        int a = 5;
                        for (int i = 0; i < 5; i++) {
                            System.out.println(a);
                            a = i * 2;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAllowedAssignment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public void test() {
                        int a = 5;
                        a = 3;

                        System.out.println(a);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


