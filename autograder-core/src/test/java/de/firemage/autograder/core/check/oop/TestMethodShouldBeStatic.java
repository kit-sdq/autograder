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

class TestMethodShouldBeStatic extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.METHOD_SHOULD_BE_STATIC,
        ProblemType.METHOD_SHOULD_BE_STATIC_NOT_PUBLIC
    );

    void assertEqualsStatic(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "method-should-be-static",
                Map.of(
                    "name", name
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMethodWithInstanceAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private int a;
                    
                    public void foo() {
                        this.a = 6;
                        System.out.println(this.a);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMethodWithImplicitInstanceAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private int a;
                    
                    public void foo() {
                        a = 6;
                        System.out.println(a);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMethodEmpty() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public void foo() {}
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsStatic(problems.next(), "foo");

        problems.assertExhausted();
    }

    @Test
    void testThisObjectAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private int a;
                    
                    public Test makeTest() {
                        Test result = new Test();
                        result.a = 5;
                        
                        return result;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsStatic(problems.next(), "makeTest");

        problems.assertExhausted();
    }

    @Test
    void testStaticMethodInvocation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static void foo() {}
                    
                    public void run() {
                        foo();
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsStatic(problems.next(), "run");

        problems.assertExhausted();
    }


    @Test
    void testExecutableReference() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Function;

                public class Test {
                    private String label;
                    
                    private String string(String input) {
                        return input + label;
                    }

                    private static void make(Function<String, String> f) {
                        System.out.println(f.apply("Hello"));
                    }
                    
                    public void run() {
                        make(this::string);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSuperAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Function;

                class Parent {
                    protected String label;
                }

                public class Test extends Parent {
                    public void run() {
                        System.out.println(super.label);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticFieldAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static String label;

                    public void run() {
                        System.out.println(label);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsStatic(problems.next(), "run");

        problems.assertExhausted();
    }

    @Test
    void testOverriddenMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Parent",
                    """
                        public class Parent {
                            // This method is overridden by Test
                            public void foo() {}
                        }
                        """
                ),
                Map.entry(
                    "Test",
                    """
                    public class Test extends Parent {
                        @Override
                        public void foo() {
                            System.out.println("Hello world");
                        }

                        public static void main(String[] args) {}
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testInterface() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public interface Test {
                    default void run() {}

                    void foo();
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStandaloneThis() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Response",
            """
                public enum Response {
                    IN_PROGRESS,
                    SUCCESS,
                    FAILURE;
                    
                    public boolean isFinished() {
                        return this == SUCCESS || this == FAILURE;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testEnumInvocationWithThis() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Response",
            """
                import java.util.Map;

                public enum Response {
                    IN_PROGRESS,
                    SUCCESS,
                    FAILURE;
                    
                    private static final Map<Response, Response> NEXT = Map.of(
                        IN_PROGRESS, SUCCESS,
                        SUCCESS, FAILURE,
                        FAILURE, FAILURE
                    );

                    public Response getNext() {
                        return NEXT.get(this);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testImplicitThisMethodCall() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Response",
            """
                public enum Response {
                    IN_PROGRESS,
                    SUCCESS,
                    FAILURE;
                    
                    public void doSomething() {
                        System.out.println("Response was: " + this.toString());
                    }

                    public Response getNext() {
                        doSomething();
                        return SUCCESS;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testEmptyConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Data",
            """
                public class Data {
                    public Data() {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


