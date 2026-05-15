package de.firemage.autograder.core.check.complexity;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestExtendsObjectCheck extends AbstractCheckTest {
    @Test
    void testExtendsObject() throws LinterException, IOException {
        List<Problem> problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            "public class Test extends Object {}"
        ), List.of(ProblemType.EXPLICITLY_EXTENDS_OBJECT));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.EXPLICITLY_EXTENDS_OBJECT, problems.get(0).getProblemType());
    }

    @Test
    void testNotExtendsObject() throws LinterException, IOException {
        List<Problem> problems = super.check(StringSourceInfo.fromSourceString(
            "Test",
            "public class Test {}"
        ), List.of(ProblemType.EXPLICITLY_EXTENDS_OBJECT));


        assertEquals(0, problems.size());
    }
}


