package de.firemage.autograder.extra.check;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCheckConflict extends AbstractCheckTest {
    @Test
    void testMissingTypeAscriptionConflict() throws LinterException, IOException {
        // See https://github.com/Feuermagier/autograder/issues/672
        // and https://github.com/Feuermagier/autograder/issues/636
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.ArrayList;
                import java.util.List;

                public class Main {
                    private List<Integer> myList = new ArrayList();
                    
                    public static void main(String[] args) {
                    }
                }
                """
        ), List.of(
            ProblemType.UNUSED_DIAMOND_OPERATOR,
            ProblemType.DO_NOT_USE_RAW_TYPES,
            ProblemType.UNCHECKED_TYPE_CAST
        ));

        assertEquals(ProblemType.DO_NOT_USE_RAW_TYPES, problems.next().getProblemType());

        problems.assertExhausted();
    }
}
