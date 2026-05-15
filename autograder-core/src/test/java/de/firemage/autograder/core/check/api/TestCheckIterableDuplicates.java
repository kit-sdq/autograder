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

class TestCheckIterableDuplicates extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "common-reimplementation";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMMON_REIMPLEMENTATION_ITERABLE_DUPLICATES);

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
    void testContainsImplicitElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;
                import java.util.HashSet;
                import java.util.List;

                public class Test {
                    public static void main(String[] args) {
                    }

                    private static boolean hasDuplicates(List<String> list) {
                        Set<String> uniqueElements = new HashSet<>();

                        for (String element : list) {
                            if (uniqueElements.contains(element)) {
                                return true; // Found a duplicate
                            }

                            uniqueElements.add(element);
                        }

                        return false; // No duplicates found
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "new HashSet<>(list).size() != list.size()");

        problems.assertExhausted();
    }

    @Test
    void testContainsExplicitElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;
                import java.util.HashSet;
                import java.util.List;

                public class Test {
                    public static void main(String[] args) {
                    }

                    private static boolean hasDuplicates(List<String> list) {
                        Set<String> uniqueElements = new HashSet<>();

                        for (String element : list) {
                            if (uniqueElements.contains(element)) {
                                return true; // Found a duplicate
                            } else {
                                uniqueElements.add(element);
                            }
                        }

                        return false; // No duplicates found
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "new HashSet<>(list).size() != list.size()");

        problems.assertExhausted();
    }

    @Test
    void testContainsAfterIf() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;
                import java.util.HashSet;
                import java.util.List;

                public class Test {
                    public static void main(String[] args) {
                    }

                    private static boolean hasDuplicates(List<String> list) {
                        Set<String> uniqueElements = new HashSet<>();

                        for (String element : list) {
                            uniqueElements.add(element);

                            if (uniqueElements.contains(element)) {
                                return true; // Found a duplicate
                            }
                        }

                        return false; // No duplicates found
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testReturnList() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;
                import java.util.HashSet;
                import java.util.List;

                public class Test {
                    public static void main(String[] args) {
                    }
                    
                    public boolean hasNoDuplicates(List<String> list) {
                        Set<String> set = new HashSet<>();
                        for (String s : list) {
                            if (!set.add(s)) {
                                return false; //# not ok
                            }
                        }
                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "new HashSet<>(list).size() == list.size()");

        problems.assertExhausted();
    }

    @Test
    void testReturnArray() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;
                import java.util.HashSet;
                import java.util.List;

                public class Test {
                    public static void main(String[] args) {
                    }
                    
                    public boolean hasNoDuplicates(String[] array) {
                        HashSet<String> set = new HashSet<>();
                        for (String s : array) {
                            if (!set.add(s)) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "new HashSet<>(Arrays.asList(array)).size() == array.length");

        problems.assertExhausted();
    }

    @Test
    void testAssignmentList() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;
                import java.util.HashSet;
                import java.util.List;

                public class Test {
                    public static void main(String[] args) {
                    }
                    
                    public boolean hasNoDuplicates(List<String> list) {
                        boolean hasDuplicates = false;
                        
                        Set<String> set = new HashSet<>();
                        for (String s : list) {
                            if (!set.add(s)) {
                                hasDuplicates = true;
                                break;
                            }
                        }

                        return !hasDuplicates;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertReimplementation(problems.next(), "new HashSet<>(list).size() != list.size()");

        problems.assertExhausted();
    }
}


