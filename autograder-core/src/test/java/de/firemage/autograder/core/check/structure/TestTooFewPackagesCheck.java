package de.firemage.autograder.core.check.structure;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestTooFewPackagesCheck extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "too-few-packages";


    void assertEqualsTooFewPackages(Problem problem) {
        assertEquals(ProblemType.TOO_FEW_PACKAGES, problem.getProblemType());
        assertEquals(
                this.linter.translateMessage(new LocalizedMessage(
                        LOCALIZED_MESSAGE_KEY,
                        Map.of("max", TooFewPackagesCheck.MAX_CLASSES_PER_PACKAGE))),
                this.linter.translateMessage(problem.getExplanation())
        );
        assertEquals(ProblemType.TOO_FEW_PACKAGES, problem.getProblemType(), "Wrong problem type");
    }
    @Test
    void test() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
                Map.ofEntries(
                        dummySourceEntry("edu.kit", "First"),
                        dummySourceEntry("edu.kit", "Second"),
                        dummySourceEntry("edu.kit", "Third"),
                        dummySourceEntry("edu.kit", "Fourth"),
                        dummySourceEntry("edu.kit", "Fifth"),
                        dummySourceEntry("edu.kit", "Sixth"),
                        dummySourceEntry("edu.kit", "Seventh"),
                        dummySourceEntry("edu.kit", "Eighth"),
                        dummySourceEntry("edu.kit", "Ninth")
                        )
        ), List.of(ProblemType.TOO_FEW_PACKAGES));

        assertTrue(problems.hasNext(), "At least one problem should be reported");
        assertEqualsTooFewPackages(problems.next());
        problems.assertExhausted();

    }

    // if there are multiple packages, the check should not be triggered
    @Test
    void testWithMultiplePackages() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
                Map.ofEntries(
                        dummySourceEntry("edu.kit", "First"),
                        dummySourceEntry("edu.kit", "Second"),
                        dummySourceEntry("edu.kit", "Third"),
                        dummySourceEntry("edu.kit", "Fourth"),
                        dummySourceEntry("edu.kit", "Fifth"),
                        dummySourceEntry("edu.kit", "Sixth"),
                        dummySourceEntry("edu.kit", "Seventh"),
                        dummySourceEntry("edu.kit", "Eighth"),
                        dummySourceEntry("edu.kit", "Ninth"),
                        dummySourceEntry("test", "Test")
                        )
        ), List.of(ProblemType.TOO_FEW_PACKAGES));
        problems.assertExhausted();
    }
    @Test
    void testWithAllowedNumberOfClasses() throws IOException, LinterException {
         ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
                Map.ofEntries(
                        dummySourceEntry("edu.kit", "First"),
                        dummySourceEntry("edu.kit", "Second"),
                        dummySourceEntry("edu.kit", "Third"),
                        dummySourceEntry("edu.kit", "Fourth"),
                        dummySourceEntry("edu.kit", "Fifth"),
                        dummySourceEntry("edu.kit", "Sixth"),
                        dummySourceEntry("edu.kit", "Seventh"),
                        dummySourceEntry("edu.kit", "Eighth")
                        )
        ), List.of(ProblemType.TOO_FEW_PACKAGES));
        problems.assertExhausted();


    }
}


