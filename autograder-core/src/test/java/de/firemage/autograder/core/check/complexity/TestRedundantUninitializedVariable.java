package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestRedundantUninitializedVariable  extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "redundant-uninitialized-variable";

    @Test
    void testMessage() throws LinterException, IOException {
        List<Problem> problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            "public class Test { void foo() { final String i; /* some comment */ i = \"hello\"; } }"
        ), List.of(ProblemType.REDUNDANT_UNINITIALIZED_VARIABLE));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.REDUNDANT_UNINITIALIZED_VARIABLE, problems.get(0).getProblemType());
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                LOCALIZED_MESSAGE_KEY,
                Map.of(
                    "variable", "i",
                    "value", "\"hello\"",
                    "suggestion", "final String i = \"hello\""
                )
            )),
            this.linter.translateMessage(problems.get(0).getExplanation())
        );
    }
}


