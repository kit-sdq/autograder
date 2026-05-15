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

class TestReassignedParameterCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REASSIGNED_PARAMETER);

    void assertEqualsReassigned(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "reassigned-parameter",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    <T> void test(int a, final int b, int c, T d) {
                        a = 1; //# not ok
                        d = null; //# not ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReassigned(problems.next(), "a");
        assertEqualsReassigned(problems.next(), "d");
        problems.assertExhausted();
    }

    @Test
    void testConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test<T> {
                    int a;
                    int b;
                    int c;
                    T d;

                    Test(int a, final int b, int c, T d) {
                        this.a = 1; //# ok
                        this.b = 1; //# ok
                        this.c = 1; //# ok
                        this.d = null; //# ok
                        
                        a = 1; //# not ok
                        this.b = b; //# ok
                        this.c = c; //# ok
                        d = null; //# not ok
                        this.d = d; //# ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReassigned(problems.next(), "a");
        assertEqualsReassigned(problems.next(), "d");
        problems.assertExhausted();
    }

    @Test
    void testRecordConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                record Test(int a) {
                    Test {
                        a = 1; //# ok
                    }

                    Test(int a, int b) {
                        this(a);
                        b = 3; //# not ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReassigned(problems.next(), "b");
        problems.assertExhausted();
    }

    @Test
    void testLambda() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.BiConsumer;

                class Test {
                    public static void main(String[] args) {
                        BiConsumer<String, Integer> f = (a, b) -> {
                            a = ""; //# not ok
                            System.out.println(a);
                            System.out.println(b);
                        };
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReassigned(problems.next(), "a");
        problems.assertExhausted();
    }
}


