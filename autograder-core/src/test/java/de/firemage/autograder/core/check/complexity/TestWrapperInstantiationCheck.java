package de.firemage.autograder.core.check.complexity;

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

class TestWrapperInstantiationCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.PRIMITIVE_WRAPPER_INSTANTIATION);

    void assertEqualsInstantiation(Problem problem, String original, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "suggest-replacement",
                Map.of(
                    "suggestion", suggestion,
                    "original", original
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        quoteCharacter = '`',
        value = {
            " Type          | Argument      | Suggestion                  ",
            " Double        | 1.0           | 1.0                         ",
            " Float         | 1.0F          | 1.0F                        ",
            " Long          | 1L            | 1L                          ",
            " Integer       | 1             | 1                           ",
            " Short         | (short) 1     | (short) 1                   ",
            " Byte          | (byte) 1      | (byte) 1                    ",
            " Character     | 'a'           | 'a'                         ",
            " Boolean       | true          | true                        ",
            //
            " Double        | \"2\"         | Double.valueOf(\"2\")       ",
            " Float         | \"2\"         | Float.valueOf(\"2\")        ",
            " Float         | 2.1           | Float.valueOf(2.1)          ",
            " Long          | \"2\"         | Long.valueOf(\"2\")         ",
            " Integer       | \"2\"         | Integer.valueOf(\"2\")      ",
            " Short         | \"2\"         | Short.valueOf(\"2\")        ",
            " Byte          | \"2\"         | Byte.valueOf(\"2\")         ",
            " Boolean       | \"True\"      | Boolean.valueOf(\"True\")   ",
        }
    )
    void testDetectsConstructorCallsForEveryPrimitiveType(String type, String argument, String suggestion) throws IOException, LinterException {
        String expression = "new %s(%s)".formatted(type, argument);
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            public class Test {
                public void execute() {
                    var value = %s;
                }
            }
            """.formatted(expression)
        ), PROBLEM_TYPES);

        assertEqualsInstantiation(problems.next(), expression, suggestion);

        problems.assertExhausted();
    }

    @Test
    void testShadowedClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Character",
                    """
                    public class Character {
                        public Character(String string, Integer integer) {}
                    }
                    """
                ),
                Map.entry(
                    "Test",
                    """
                    public class Test {
                        public void execute() {
                            Character character = new Character("a", 1);
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


