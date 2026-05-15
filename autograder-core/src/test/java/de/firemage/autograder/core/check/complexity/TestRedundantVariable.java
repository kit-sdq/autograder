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

class TestRedundantVariable extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REDUNDANT_VARIABLE);

    void assertEqualsRedundant(Problem problem, String name, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "redundant-variable",
                Map.of(
                    "name", name,
                    "suggestion", suggestion
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testLiteralReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get() {
                        return 5;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConstantReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final int RETURN_VALUE = 5;

                    public int get() {
                        return RETURN_VALUE;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testSimpleCase() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get() {
                        int a = 5;

                        return a;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "a", "return 5");

        problems.assertExhausted();
    }

    @Test
    void testComments() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int getA() {
                        int a = 5;
                        // comment
                        return a;
                    }
                    
                    public int getB() {
                        int b = 2; // comment
                        return b;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "a", "// comment%nreturn 5".formatted());
        assertEqualsRedundant(problems.next(), "b", "return 2");

        problems.assertExhausted();
    }

    @Test
    void testTernaryReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get() {
                        int a = 5;

                        return a > 3 ? a : 3;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMethodCall() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public void updateArray(String[] array) {
                        array[0] = "b";
                    }

                    public String[] get() {
                        String[] array = new String[1];

                        updateArray(array);

                        return array;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAssert() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get() {
                        int i = 2;

                        assert i >= 0;

                        return i;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSwitchCase() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get(int number) {
                        switch (number) {
                            default:
                                int a = 0;
                                return a;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "a", "return 0");

        problems.assertExhausted();
    }

    @Test
    void testFollowingStatementWithoutUse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get(int number) {
                        int a = 3;
                        System.out.println(number);
                        return a;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testVariableForSuppressWarning() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "TestCase",
            """
                public class TestCase<T> {
                    T getSomeT() { return null; }

                    private static <T> T findEventTypeValueByName(TestCase<?> unchecked) {
                        @SuppressWarnings("unchecked")
                        T result = (T) unchecked.getSomeT();

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMapReference() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Map;
                import java.util.HashMap;
                
                public class Test {
                    public Map<String, Integer> get(Map<String, Integer> map) {
                        Map<String, Integer> copy = new HashMap<>(map);
                        
                        map.clear();
                        
                        return copy;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testFinalVariableReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public Object get() {
                        final Object object = new Object();
                        return object;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "object", "return new Object()");

        problems.assertExhausted();
    }

    @Test
    void testMethodCapture() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Supplier;

                public class Test {
                    public Supplier<String> get() {
                        Object value = "a";
                        return value::toString;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRedundantVariableInLambda() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Supplier;

                public class Test {
                    private static final Supplier<String> SUPPLIER = () -> {
                        String value = "some value";
                        return value;
                    };
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "value", "return \"some value\"");

        problems.assertExhausted();
    }

    @Test
    void testRedundantVariableInAnonymousClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Supplier;

                public class Test {
                    private static final Supplier<String> SUPPLIER = new Supplier<>() {
                        public String get() {
                            String value = "some value";
                            return value;
                        }
                    };
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "value", "return \"some value\"");

        problems.assertExhausted();
    }

    @Test
    void testRedundantSwitchExpression() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public int get(int number) {
                        int result = switch (number) {
                            case 1 -> 2;
                            case 2 -> 3;
                            default -> 10;
                        };
                        
                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


