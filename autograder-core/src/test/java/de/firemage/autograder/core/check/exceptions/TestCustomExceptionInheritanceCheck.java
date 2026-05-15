package de.firemage.autograder.core.check.exceptions;

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

class TestCustomExceptionInheritanceCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.CUSTOM_EXCEPTION_INHERITS_ERROR,
        ProblemType.CUSTOM_EXCEPTION_INHERITS_RUNTIME_EXCEPTION
    );

    void assertEqualsRuntime(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "custom-exception-inheritance-runtime",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsError(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "custom-exception-inheritance-error",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testInheritsRuntime() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "MyException",
            """
                public class MyException extends RuntimeException {}
                """
        ), PROBLEM_TYPES);

        assertEqualsRuntime(problems.next(), "MyException");

        problems.assertExhausted();
    }

    @Test
    void testInheritsError() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "MyException",
            """
                public class MyException extends Error {}
                """
        ), PROBLEM_TYPES);

        assertEqualsError(problems.next(), "MyException");

        problems.assertExhausted();
    }
}


