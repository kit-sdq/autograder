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

class TestUseSubList extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.COMMON_REIMPLEMENTATION_SUBLIST);

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
    void testSubList() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Collection;

                public class Test {
                    public static <T> Collection<T> printList(List<T> list, int start, int end) {
                        Collection<T> result = new ArrayList<>();
                        for (int i = start; i < end; i++) {
                            result.add(list.get(i));
                        }
                        return result;
                    }
                    
                    public static Collection printRawList(List list, int start, int end) {
                        Collection result = new ArrayList();
                        for (int i = start; i < end; i++) {
                            result.add(list.get(i));
                        }
                        return result;
                    }
                    
                    public static Collection<Integer> printList2(List<Integer> list, int start, int end) {
                        Collection<Integer> result = new ArrayList<>();
                        for (int i = start; i < end; i++) {
                            result.add(list.get(i));
                        }
                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "result.addAll(list.subList(start, end))");
        assertEqualsReimplementation(problems.next(), "result.addAll(list.subList(start, end))");
        assertEqualsReimplementation(problems.next(), "result.addAll(list.subList(start, end))");
        problems.assertExhausted();
    }

    @Test
    void testEntireList() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                import java.util.Collection;

                public class Test {
                    public static Collection<String> iter(List<String> storage) {
                        Collection<String> result = new ArrayList<>();
                        for (int i = 0; i < storage.size(); i++) {
                            result.add(storage.get(i));
                        }
                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMap() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Map;
                import java.util.ArrayList;
                import java.util.Collection;

                public class Test {
                    public static Collection<String> iterMap(Map<Integer, String> storage) {
                        Collection<String> result = new ArrayList<>();
                        for (int i = 0; i < storage.size(); i++) {
                            result.add(storage.get(i));
                        }
                        return result;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}

