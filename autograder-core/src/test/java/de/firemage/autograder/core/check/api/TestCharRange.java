package de.firemage.autograder.core.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCharRange extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";

    @Test
    void testIsDigit() throws LinterException, IOException {
        var problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static boolean isNumber(char input) {
                        return input <= '9' && input >= '0';
                    }
                }
                """
        ), List.of(ProblemType.CHAR_RANGE));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.CHAR_RANGE, problems.get(0).getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "suggestion", "Character.isDigit(input)"
                    )
                )),
            this.linter.translateMessage(problems.get(0).getExplanation())
        );
    }

    @Test
    void testIsLowerCase() throws LinterException, IOException {
        var problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static boolean isLowerCase(char c) {
                        return c >= 'a' && c <= 'z';
                    }
                }
                """
        ), List.of(ProblemType.CHAR_RANGE));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.CHAR_RANGE, problems.get(0).getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "suggestion", "Character.isAlphabetic(c) && Character.isLowerCase(c)"
                    )
                )),
            this.linter.translateMessage(problems.get(0).getExplanation())
        );
    }

    @Test
    void testIsNotLowerCase() throws LinterException, IOException {
        var problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static boolean isNotLowerCase(char c) {
                        return c < 'a' || c > 'z';
                    }
                }
                """
        ), List.of(ProblemType.CHAR_RANGE));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.CHAR_RANGE, problems.get(0).getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "suggestion", "!Character.isAlphabetic(c) || !Character.isLowerCase(c)"
                    )
                )),
            this.linter.translateMessage(problems.get(0).getExplanation())
        );
    }

    @Test
    void testNormalization() throws LinterException, IOException {
        var problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static boolean isLowerCase(char c) {
                        return c > '/' && ':' > c;
                    }
                }
                """
        ), List.of(ProblemType.CHAR_RANGE));

        assertEquals(1, problems.size());
        assertEquals(ProblemType.CHAR_RANGE, problems.get(0).getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "suggestion", "Character.isDigit(c)"
                    )
                )),
            this.linter.translateMessage(problems.get(0).getExplanation())
        );
    }

    @Test
    void testIsUpperCase() throws LinterException, IOException {
        var problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static boolean isUpperCase(char c) {
                        return c >= 'A' && 'Z' >= c;
                    }
                }
                """
        ), List.of(ProblemType.CHAR_RANGE));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.CHAR_RANGE, problems.get(0).getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "suggestion", "Character.isAlphabetic(c) && Character.isUpperCase(c)"
                    )
                )),
            this.linter.translateMessage(problems.get(0).getExplanation())
        );
    }
}

