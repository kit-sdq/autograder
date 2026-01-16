package de.firemage.autograder.core.check.comment;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCommentedOutCodeCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMMENTED_OUT_CODE);

    void assertEqualsCode(Problem problem, int startLine, int endLine) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("commented-out-code")),
            this.linter.translateMessage(problem.getExplanation())
        );
        var position = problem.getPosition();
        assertEquals(startLine, position.startLine());
        assertEquals(endLine, position.endLine());
    }

    @Test
    void testInlineCode() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                JavaVersion.JAVA_17,
                "Test",
                """
                    public class Test {
                        // int a = b;

                        // if (a) {
                        //    ...
                        // }
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        assertEqualsCode(problems.next(), 2, 2);
        assertEqualsCode(problems.next(), 4, 4);
        assertEqualsCode(problems.next(), 6, 6);

        problems.assertExhausted();
    }

    @Test
    void testMultilineCode() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                JavaVersion.JAVA_17,
                "Test",
                """
                    public class Test {
                        /*
                        int a = b;
                        
                        if (a == 3) {
                            System.out.println("a is 3");
                        }
                         */
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        assertEqualsCode(problems.next(), 2, 8);

        problems.assertExhausted();
    }

    @Test
    void testCoalescing() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                JavaVersion.JAVA_17,
                "Test",
                """
                    public class Test {
                        // int a = b;

                        // if (a) {
                        //    print(a);
                        // }
                         // differentIndent();

                        // while (true) {
                        /*     a += 7;

                             */ // another = one;
                        // }
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        assertEqualsCode(problems.next(), 2, 2);
        assertEqualsCode(problems.next(), 4, 6);
        assertEqualsCode(problems.next(), 7, 7);
        assertEqualsCode(problems.next(), 9, 13);

        problems.assertExhausted();
    }

    @Test
    void testNoCodeComments() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                JavaVersion.JAVA_17,
                "RunCommand",
                """
                     /**
                     * This {@link RunCommand} executes a run on the target.
                     * 
                     * @author Programmieren-Team
                     */
                    public class RunCommand {
                        // some comment
                        
                        /*
                        A multi line comment with special character;
                        two words
                        */
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        problems.assertExhausted();
    }

    @Test
    void testMultilineCodeReal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
                StringSourceInfo.fromSourceString(
                        JavaVersion.JAVA_17,
                        "Test",
                        """
                                public class Test {
                                
                                    /*
                                    public int add() {
                                        return 1 + 1;
                                    }
                                
                                    private int subtract() {
                                        return 1 - 1;
                                    }
                                     */
                                }
                                
                            """
                ),
                PROBLEM_TYPES
        );

        assertEqualsCode(problems.next(), 3, 11);

        problems.assertExhausted();
    }
}
