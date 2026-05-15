package de.firemage.autograder.extra.check.oop;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestInheritanceBadPractices extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.ABSTRACT_CLASS_WITHOUT_ABSTRACT_METHOD,
        // ProblemType.USE_DIFFERENT_VISIBILITY,
        ProblemType.SHOULD_BE_INTERFACE,
        ProblemType.COMPOSITION_OVER_INHERITANCE
    );

    @Test
    void testCompositionMessage() throws LinterException, IOException {
        var problems = this.check(StringSourceInfo.fromSourceString(
            "Test",
            """
            public class Test {}

            abstract class ExampleParent { // Not Ok (composition over inheritance)
                private String field;
                protected ExampleParent() {
                    this.field = "test";
                }
            }

            class Subclass extends ExampleParent {
                public Subclass() {
                    super();
                }
            }
            """), List.of(ProblemType.COMPOSITION_OVER_INHERITANCE));


        assertEquals(1, problems.size());
        assertEquals(ProblemType.COMPOSITION_OVER_INHERITANCE, problems.get(0).getProblemType());
        assertEquals(this.linter.translateMessage(new LocalizedMessage("composition-over-inheritance", Map.of("suggestion", "ExampleParent exampleParent()"))), this.linter.translateMessage(problems.get(0).getExplanation()));
    }

    @Test
    void testInheritsUnimplementedInterfaceMethods() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Command",
                    """
                    public interface Command<T> {
                        T execute();
                    }
                    """
                ),
                Map.entry(
                    "FindTripsCommand",
                    """
                    public abstract class FindTripsCommand<T extends Number> implements Command<T> {
                        private static final String LINE_FORMAT = "%d Transfers: %s";
                        // does not implement any of the command methods
                    
                        public void foo() {
                            System.out.println("bar");
                        }
                    }
                    """
                ),
                Map.entry(
                    "FindShortestTripsCommand",
                    """
                    public class FindShortestTripsCommand extends FindTripsCommand<Integer> {
                        private static final String LINE_FORMAT = "%d Transfers: %s";
                        @Override
                        public Integer execute() {
                            return 0;
                        }
                    }
                    """
                )
            )), List.of(ProblemType.ABSTRACT_CLASS_WITHOUT_ABSTRACT_METHOD));

        problems.assertExhausted();
    }
}
