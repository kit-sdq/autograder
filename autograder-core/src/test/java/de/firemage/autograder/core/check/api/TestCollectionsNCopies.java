package de.firemage.autograder.core.check.api;

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

class TestCollectionsNCopies extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COLLECTIONS_N_COPIES);

    private void assertReimplementation(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of("suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testListAdd() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                public class Test {
                    public void foo(List<String> list) {
                        for (int i = 0; i < 10; i++) {
                            list.add(" ");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "list.addAll(Collections.nCopies(10, \" \"))");

        problems.assertExhausted();
    }

    @Test
    void testCollectionAdd() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Collection;

                public class Test {
                    public void foo(Collection<Integer> collection, int n) {
                        for (int i = 1; i < n; i++) {
                            collection.add(1);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "collection.addAll(Collections.nCopies(n - 1, 1))");

        problems.assertExhausted();
    }
}


