package de.firemage.autograder.core.check.naming;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestBooleanIdentifierCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.BOOLEAN_GETTER_NOT_CALLED_IS);

    private void assertName(Problem problem, String methodName, String newName) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "bool-getter-name",
                    Map.of(
                        "oldName", methodName,
                        "newName", newName
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testGetter() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public boolean getValue() { /*# not ok #*/
                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertName(problems.next(), "getValue", "isValue");
        problems.assertExhausted();
    }

    @Test
    void testCorrectlyNamedGetter() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public boolean hasValue() {
                        return true;
                    }

                    public boolean isValue() {
                        return true;
                    }
                    
                    public boolean value() {
                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testBooleanArray() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public boolean[] getValue() {
                        return null;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


