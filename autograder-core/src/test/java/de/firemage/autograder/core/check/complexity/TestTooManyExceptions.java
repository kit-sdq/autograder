package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTooManyExceptions  extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.TOO_MANY_EXCEPTIONS);

    private static final List<String> EXCEPTION_NAMES = List.of(
        "InvalidArgumentException", "ModelException", "InvalidCommandException",
        "BuildException", "DrawException", "InvalidNameException",
        "QuitException", "InvalidGameException", "TooManyPlayersException",
        "InvalidPlayerException", "InvalidMoveException", "InvalidPositionException"
    );

    void assertEqualsTooMany(Problem problem, int count) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "too-many-exceptions",
                Map.of("count", count)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    private static String makeException(String name, String parent) {
        return """
            public class %s extends %s {
                public %s(String message) {
                    super(message);
                }
            }
            """.formatted(name, parent, name);
    }

    private static Map<String, String> makeNExceptions(int exceptions, int runtimeExceptions) {
        Map<String, String> result = new HashMap<>();

        for (String name : EXCEPTION_NAMES.subList(0, exceptions)) {
            result.put(name, makeException(name, "Exception"));
        }

        for (String name : EXCEPTION_NAMES.subList(exceptions, exceptions + runtimeExceptions)) {
            result.put(name, makeException(name, "RuntimeException"));
        }

        return result;
    }

    @Test
    void testTooManyExceptions() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceStrings(makeNExceptions(4, 2)),
            PROBLEM_TYPES
        );

        assertEqualsTooMany(problems.next(), 6);

        problems.assertExhausted();
    }

    @Test
    void testNotTooManyExceptions() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceStrings(makeNExceptions(4, 1)),
            PROBLEM_TYPES
        );

        problems.assertExhausted();
    }
}
