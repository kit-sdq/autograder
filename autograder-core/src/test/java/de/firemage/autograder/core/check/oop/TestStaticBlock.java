package de.firemage.autograder.core.check.oop;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestStaticBlock extends AbstractCheckTest {
    private void assertEqualsStaticBlock(Problem problem) {
        assertEquals(ProblemType.AVOID_STATIC_BLOCKS, problem.getProblemType());
        assertEquals(
                this.linter.translateMessage(new LocalizedMessage(CheckStaticBlocks.LOCALIZED_MESSAGE_KEY)),
                this.linter.translateMessage(problem.getExplanation()));
    }
    @Test
    void testStaticBlockOutside() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Test",
                """
                    import java.util.ArrayList;
                    import java.util.List;
                    
                    public class Test {
                        private static final List<String> list = new ArrayList<>();
                        static {
                            list.add("initialise");
                        }
                    }
                    """
        ), List.of(ProblemType.AVOID_STATIC_BLOCKS));
        assertTrue(problems.hasNext(), "At least one problem expected");
        assertEqualsStaticBlock(problems.next());
        problems.assertExhausted();
    }
    @Test
    void testBlockInStaticMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Test",
                """
                    public class Test {
                        public static void main(String ... args) {
                            {
                                System.out.println("Hello World");
                            }
                        } 
                    }
                    """
        ), List.of(ProblemType.AVOID_STATIC_BLOCKS));
        problems.assertExhausted();
    }
    @Test
    void testNormalBlocks() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Test",
                """
                    public class Test {
                    {
                        System.out.println("Hello World");
                    }
                 
                    }
                    """
        ), List.of(ProblemType.AVOID_STATIC_BLOCKS));
        problems.assertExhausted();
    }

    @Test
    void testNamedAndNormal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Test",
                """
                    public class Test {
                        public static void main(String... args) {
                            {
                                System.out.println("Hello World");
                            }
                            name: {
                                System.out.println("Hello World");
                            }
                        }
                    }
                    """
        ), List.of(ProblemType.AVOID_STATIC_BLOCKS));
        problems.assertExhausted();
    }


}


