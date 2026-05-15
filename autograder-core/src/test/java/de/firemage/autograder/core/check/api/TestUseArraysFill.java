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

class TestUseArraysFill extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMMON_REIMPLEMENTATION_ARRAYS_FILL);

    private void assertEqualsReimplementation(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of(
                        "suggestion", suggestion
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testArraysFill() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.Arrays;

                public class Main {
                    private static final String INITIAL_VALUE = "X";

                    public static void init(String[] array) {
                        for (int i = 0; i < array.length; i++) {
                            array[i] = INITIAL_VALUE;
                        }
                        
                        for (int i = 1; i < array.length; i++) {
                            array[i] = INITIAL_VALUE;
                        }
                        
                        for (int i = 0; i < array.length; i++) {
                            array[i] = INITIAL_VALUE + i; // ignored because it uses i
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "Arrays.fill(array, INITIAL_VALUE)");
        assertEqualsReimplementation(problems.next(), "Arrays.fill(array, 1, array.length, INITIAL_VALUE)");
        problems.assertExhausted();
    }

    // See https://github.com/Feuermagier/autograder/issues/245
    @Test
    void testArraysFillMutableClass() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;

                public class Test {
                    public record Cell(List<String> list) {
                        public Cell() {
                            this(new ArrayList<>());
                        }

                        public void add(String string) { this.list.add(string); }
                    }

                    public static Cell[] createCells(int n) {
                        Cell[] result = new Cell[n];

                        for (int i = 0; i < result.length; i++) {
                            result[i] = new Cell();
                        }

                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testArraysFillRecursiveType() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    import java.util.Arrays;

                    class PlayingFieldEntry {
                        static final PlayingFieldEntry FREE = new PlayingFieldEntry();
                    }

                    public class Main {
                        public static void main(String[] args) {
                            PlayingFieldEntry[] field = new PlayingFieldEntry[1];

                            for (int i = 0; i < field.length; i++) {
                                field[i] = PlayingFieldEntry.FREE;
                            }
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testHasNoJavaUtilImports() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    class PlayingFieldEntry {
                        static final PlayingFieldEntry FREE = new PlayingFieldEntry();
                    }

                    public class Main {
                        public static void main(String[] args) {
                            PlayingFieldEntry[] field = new PlayingFieldEntry[1];

                            for (int i = 0; i < field.length; i++) {
                                field[i] = PlayingFieldEntry.FREE;
                            }
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testAssignedInvocation() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    import java.util.LinkedList;

                    public class Main {
                        public static int[] reversePath(LinkedList<Integer> orderedPath) {
                            int[] reversePath = new int[orderedPath.size()];
                            int size = orderedPath.size();
                            for (int i = 0; i < size; i++) {
                                reversePath[i] = orderedPath.removeFirst();
                            }
                            
                            return reversePath;
                        }

                        public static void main(String[] args) {
                            System.out.println(reversePath(new LinkedList<>()));
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


