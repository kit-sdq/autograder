package de.firemage.autograder.core.check.oop;
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

class TestMethodShouldBeAbstract extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.METHOD_USES_PLACEHOLDER_IMPLEMENTATION);

    void assertShouldBeAbstract(Problem problem, String type, String method) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "method-should-be-abstract",
                Map.of(
                    "type", type,
                    "method", method
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testPrivateMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public abstract class Test {
                    private Object privateNull() { /*# ok; private #*/
                        return null;
                    }

                    private Object privateInstance() { /*# ok; private #*/
                        return new Object();
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testAbstractMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public abstract class Test {
                    public abstract Object abstractMethod(); /*# ok; abstract #*/
                    
                    public static void main(String[] args) {
                        var test = new Test() {
                            @Override
                            public Object abstractMethod() {
                                return null;
                            }
                        };
                        
                        test.abstractMethod();
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public abstract class Test {
                    public static Object staticNull() { /*# ok; static #*/
                        return null;
                    }
                    
                    public static void main(String[] args) {
                        var test = new Test() {};
                        
                        test.staticNull();
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testOverriddenMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public abstract class Test {
                    @Override
                    public String toString() { /*# ok; overrides #*/
                        return null;
                    }
                    
                    public static void main(String[] args) {
                        var test = new Test() {
                            @Override
                            public String toString() {
                                return "abc";
                            }
                        };
                        
                        System.out.println(test.toString());
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Return Type   | Arguments  | Body                                        | Expected     ",
            " Object        |            | return null;                                | foo          ",
            " Object        | int x      | return null;                                | foo          ",
            " Object        |            | return new Object();                        |              ",
            " void          |            |                                             | foo          ",
            " void          | int x      |                                             | foo          ",
            " void          |            | throw new IllegalStateException();          | foo          ",
            " void          |            | throw new UnsupportedOperationException();  | foo          ",
            " void          | int x      | throw new IllegalStateException();          | foo          ",
            " Object        |            | if (1 < 2) throw new IllegalStateException(); return null;  |        ",
        }
    )
    void testShouldBeAbstract(String returnType, String arguments, String body, String expected) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public abstract class Test {
                    public %s foo(%s) { %s }
                    
                    public static void main(String[] args) {
                        var test = new Test() {
                            @Override
                            public %s foo(%s) {
                                %s
                            }
                        };

                        test.foo(%s);
                    }
                }
                """.formatted(
                returnType, arguments == null ? "" : arguments, body == null ? "" : body,
                returnType, arguments == null ? "" : arguments, body == null ? "" : body,
                arguments == null ? "" : "1"
            )
        ), PROBLEM_TYPES);

        if (expected != null) {
            assertShouldBeAbstract(problems.next(), "Test", expected);
        }

        problems.assertExhausted();
    }

    @Test
    void testCalledInSubclass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Command",
            """
                public abstract class Command {
                    protected void policyError() throws IllegalStateException {
                        throw new IllegalStateException("Command was run against availability policy.");
                    }
                    
                    public static class RollDiceCommand extends Command {
                        public void rollDice(String session) throws IllegalStateException {
                            if (session == null) {
                                super.policyError();
                            }
                        }
                    }

                    public static void main(String[] args) {
                        var test = new RollDiceCommand();
                        
                        test.rollDice(null);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

}


