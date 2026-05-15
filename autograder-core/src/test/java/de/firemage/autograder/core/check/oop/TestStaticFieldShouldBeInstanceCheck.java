package de.firemage.autograder.core.check.oop;

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

class TestStaticFieldShouldBeInstanceCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.STATIC_FIELD_SHOULD_BE_INSTANCE);

    void assertShouldBeInstance(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "static-field-should-be-instance",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testShouldBeInstance() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    private static String a = "a"; //# not ok
                    private static String b = "b"; //# ok, effectively final

                    public static void main(String[] args) {
                        a = "c";

                        System.out.println(a);
                        System.out.println(b);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertShouldBeInstance(problems.next(), "a");

        problems.assertExhausted();
    }

    @Test
    void testGlobalCounter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Customer",
            """
                public class Customer {
                    private static int counter = 1; //# ok
                    private int customerNumber;

                    public Customer() {
                        this.customerNumber = counter++;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


