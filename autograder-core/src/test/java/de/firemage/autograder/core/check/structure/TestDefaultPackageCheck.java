package de.firemage.autograder.core.check.structure;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestDefaultPackageCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.DEFAULT_PACKAGE_USED
    );

    private void assertEqualsDefaultPackageUsed(Problem problem, String positions) {
        assertEquals(ProblemType.DEFAULT_PACKAGE_USED, problem.getProblemType());

        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("default-package", Map.of("positions", positions))),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void test() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello World!");
                }
            }
            """
        ), PROBLEM_TYPES);


        assertEqualsDefaultPackageUsed(problems.next(), "Test:L1");
        problems.assertExhausted();
    }

    @Test
    void testMultipleClasses() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                dummySourceEntry("com.example", "Test"),
                dummySourceEntry("", "Hello"),
                dummySourceEntry("", "World")
            )
        ), PROBLEM_TYPES);

        assertEqualsDefaultPackageUsed(problems.next(), "Hello:L1, World:L1");
        problems.assertExhausted();
    }

    @Test
    void testNoDefaultPackageUsed() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                dummySourceEntry("com.example", "Test"),
                dummySourceEntry("com.example.a", "Hello"),
                dummySourceEntry("com.example.b", "World")
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
