package de.firemage.autograder.core.check.comment;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUnnecessaryComment extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.UNNECESSARY_COMMENT);

    void assertEqualsEmpty(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("unnecessary-comment-empty")),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testPlaceholder() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                "Test",
                """
                    public class Test {
                        public Test() {
                            //
                        }
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        assertEqualsEmpty(problems.next());

        problems.assertExhausted();
    }

    @Test
    void testCommentSpacer() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                "Test",
                """
                    public class Test {
                        public Test() {
                            // Here is a very long explanation
                            //
                            // ^^ here the line is empty and valid for spacing
                        }
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        problems.assertExhausted();
    }

    @Test
    void testVeryLongComment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                "Test",
                """
                    public class Test {
                        public Test() {
                            // One of the difficulties of this task is parsing the command line arguments correctly.
                            // In many assignments the commands are given as a single word like "jaccard" or "quit"
                            // and commands with multiple words would be joined by an underscore like "add_author" or
                            // "publications-by".
                            //
                            // In this assignment some commands like "add author" or "publications by" contain spaces
                            // and not all arguments are separated by space like "add journal <journal>,<publisher>".
                            //
                            // This makes splitting the input by a single character (like a space) difficult, because for example
                            // > add journal Science Direct,Elsevier
                            // is a valid input and should be parsed into ["add journal", "Science Direct", "Elsevier"]
                            //
                            // To solve this problem, we check if our input starts with the command name (for example "add author")
                            // and we then remove the command name from the input and pass the rest as arguments to the command.
                            String commandWithArguments = "add author John Doe";
                        }
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        problems.assertExhausted();
    }

    @Test
    void testPartiallyFilledJavadoc() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                "edu.kit.kastel.Main",
                """
                    package edu.kit.kastel;

                    /**
                     *
                     * @author uxxxx
                     */
                    public class Main {
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        problems.assertExhausted();
    }

    @Test
    void testPartiallyFilledJavadocNoPackage() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(
            StringSourceInfo.fromSourceString(
                "Main",
                """
                    /**
                     *
                     * @author uxxxx
                     */
                    public class Main {
                    }
                    """
            ),
            PROBLEM_TYPES
        );

        problems.assertExhausted();
    }
}
