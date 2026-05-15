package de.firemage.autograder.core.check.general;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestMagicLiteral extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.MAGIC_LITERAL);

    void assertMagicLiteral(Problem problem, String value) {
        String type = "number";
        if (value.contains("'")) {
            type = "character";
        } else if (value.contains("\"")) {
            type = "string";
        }

        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "magic-literal",
                Map.of(
                    "type", type,
                    "value", value
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    private static final String NON_MAGIC_STRING = "Hello World!"; //# ok
                    
                    public static void main(String[] args) {
                        String magicString = "Hello World"; //# not ok
                        String empty = "" + 1; //# ok
                    }
                    
                    private static void doSomething(String string) {
                        if (string.equals("value")) { //# not ok
                            throw new IllegalStateException("some error message"); //# not ok
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertMagicLiteral(problems.next(), "\"Hello World\"");
        assertMagicLiteral(problems.next(), "\"value\"");
        assertMagicLiteral(problems.next(), "\"some error message\"");

        problems.assertExhausted();
    }

    @Test
    void testEnumConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                enum Test {
                    MONDAY("Monday"), //# ok
                    TUESDAY("Tuesday"), //# ok
                    WEDNESDAY("Wednesday"), //# ok
                    THURSDAY("Thursday"), //# ok
                    FRIDAY("Friday"), //# ok
                    SATURDAY("Saturday"), //# ok
                    SUNDAY("Sunday"); //# ok
                    
                    private final String name;
                    
                    Test(String name) {
                        this.name = name;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testEnumLambda() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "EnumTest",
            """
                import java.util.function.Function;

                public enum EnumTest {
                    WEDNESDAY(string -> {
                        return "NotThursday1"; //# ok
                    }, "Thursday"); //# ok

                    public static final String OTHER = "other"; //# ok
                    private final Function<String, String> function;
                    private final String next;

                    EnumTest(Function<String, String> function, String next) {
                        this.function = string -> {
                            return "NotThursday2"; //# not ok
                        };
                        this.next = next;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertMagicLiteral(problems.next(), "\"NotThursday2\"");

        problems.assertExhausted();
    }

    @Test
    void testDoubleBraceInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Map;
                import java.util.HashMap;

                public class Test {
                    private static final Map<Object, Object> DOUBLE_BRACE_INIT = new HashMap<>() {{
                        put("first", 1); //# ok
                        put(2, "second"); //# ok
                        put("both", "sides"); //# ok
                    }};
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMagicStringsInCode() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Function;

                public class Test {
                    public static void main(String[] args) {
                        String magicString = "Hello World"; //# not ok
                        String empty = "" + 1; //# ok
                    }

                    private static void doSomething(String string) {
                        if (string.equals("value")) { //# not ok
                            throw new IllegalStateException("some error message"); //# not ok
                        }
                    }

                    private Function<String, String> getFunction() {
                        return string -> "whatever"; //# not ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertMagicLiteral(problems.next(), "\"Hello World\"");
        assertMagicLiteral(problems.next(), "\"value\"");
        assertMagicLiteral(problems.next(), "\"some error message\"");
        assertMagicLiteral(problems.next(), "\"whatever\"");

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression     | IsMagicLiteral ",
            " \"a\"          | true           ",
            " \"\"           | false          ",
            " 0              | false          ",
            " 1              | false          ",
            " 2              | false          ",
            " -1             | false          ",
            " -2             | false          ",
            //
            " -0.0f          | false          ",
            " 0.0f           | false          ",
            " 1.0f           | false          ",
            " 1.1F           | true           ",
            " 0.0            | false          ",
        }
    )
    void testValues(String expression, boolean isMagicLiteral) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            public class Test {
                public static void main(String[] args) {
                    var value = %s;
                }
            }
            """.formatted(expression)
        ), PROBLEM_TYPES);

        if (isMagicLiteral) {
            assertMagicLiteral(problems.next(), expression);
        }

        problems.assertExhausted();

    }

    @Test
    void testMagicNumber() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.function.Function;

                public class Test {
                    public static void main(String[] args) {
                        float magicFloat = 1.0f; //# ok
                        String empty = "" + 5;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertMagicLiteral(problems.next(), "5");

        problems.assertExhausted();
    }
}


