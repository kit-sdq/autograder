package de.firemage.autograder.core.check.oop;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestInstanceof extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.INSTANCEOF, ProblemType.INSTANCEOF_EMULATION);

    void assertInstanceof(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("do-not-use-instanceof")),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertInstanceofEmulation(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("do-not-use-instanceof-emulation")),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testRegularInstanceof() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void checkObject(Object obj) {
                        if (obj instanceof String) {
                            System.out.println("It's a string!");
                        } else if (obj instanceof Integer) {
                            System.out.println("It's an integer!");
                        } else {
                            System.out.println("Unknown type");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertInstanceof(problems.next());
        assertInstanceof(problems.next());

        problems.assertExhausted();
    }

    @Test
    void testInstanceofInEquals() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    @Override
                    public boolean equals(Object obj) {
                        if (obj instanceof Test) {
                            return true;
                        }
                        return false;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testGetClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void checkObject(Object obj) {
                        if (obj.getClass().equals(String.class)) {
                            System.out.println("It's a string!");
                        } else {
                            System.out.println("Unknown type");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertInstanceofEmulation(problems.next());

        problems.assertExhausted();
    }

    @Test
    void testTryCatch() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void checkObject(Object obj) {
                        try {
                            String string = (String) obj;
                            System.out.println("It's a string!");
                        } catch (ClassCastException ignored) { /*# not ok #*/
                            System.out.println("It's not a string!");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertInstanceofEmulation(problems.next());

        problems.assertExhausted();
    }


    @Test
    void testGetClassInToString() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    @Override
                    public String toString() {
                        return this.getClass().getSimpleName();
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testAssignableFrom() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                // I am not sure how one can call isAssignableFrom, without using getClass
                // but there had been one report where that seemed to be the case. I sadly
                // did not make a test case from that.
                public class Test {
                    public static void checkObject(Object obj) {
                        if (String.class.isAssignableFrom(obj.getClass())) {
                            System.out.println("It's a string!");
                        } else {
                            System.out.println("Unknown type");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertInstanceofEmulation(problems.next());
        assertInstanceofEmulation(problems.next());

        problems.assertExhausted();
    }
}


