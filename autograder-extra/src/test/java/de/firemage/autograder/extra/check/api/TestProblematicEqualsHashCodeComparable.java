package de.firemage.autograder.extra.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestProblematicEqualsHashCodeComparable extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.COMPARE_TO_ZERO,
        ProblemType.EQUALS_USING_HASHCODE,
        ProblemType.EQUALS_UNSAFE_CAST,
        ProblemType.EQUALS_INCOMPATIBLE_TYPE,
        ProblemType.INCONSISTENT_HASH_CODE,
        ProblemType.UNDEFINED_EQUALS,
        ProblemType.NON_OVERRIDING_EQUALS,
        ProblemType.EQUALS_BROKEN_FOR_NULL,
        ProblemType.ARRAYS_HASHCODE,
        ProblemType.EQUALS_REFERENCE,
        ProblemType.ARRAY_AS_KEY_OF_SET_OR_MAP
    );

    void assertCompareToZero(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "compare-to-zero"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsUsingHashCode(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "equals-using-hashcode"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsUnsafeCast(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "equals-unsafe-cast"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsIncompatibleType(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "equals-incompatible-type"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertInconsistentHashCode(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "inconsistent-hashcode"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertUndefinedEquals(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "undefined-equals"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertNonOverridingEquals(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "non-overriding-equals"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsBrokenForNull(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "equals-broken-for-null"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertArrayHashCode(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "array-hash-code"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsReference(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "equals-reference"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertArraysAsKeyOfSetOrMap(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "array-as-key-of-set-or-map"
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testComparator() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Test",
                    """
                        import java.util.*;

                        public class Test {
                            public static void main(String[] args) {}
                        
                            static <T> boolean isLessThan(Comparator<T> comparator, T a, T b) {
                                // Fragile: it's not guaranteed that `comparator` returns -1 to mean
                                // "less than".
                                return comparator.compare(a, b) == -1; /*# not ok #*/
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertCompareToZero(problems.next());

        problems.assertExhausted();
    }


    @Test
    void testEqualsUsingHashCode() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Test",
                    """
                        import java.util.*;

                        public class Test {
                            private int a;
                            private int b;
                            private String c;

                            @Override
                            public boolean equals(Object o) { /*# not ok #*/
                                return o.hashCode() == hashCode(); /*# not ok #*/
                            }

                            @Override
                            public int hashCode() {
                                return Objects.hash(a, b, c);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsUsingHashCode(problems.next());
        assertEqualsBrokenForNull(problems.next());

        problems.assertExhausted();
    }

    @Test
    void testEqualsUnsafeCast() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Test",
                    """
                        import java.util.*;

                        public class Test {
                            private int a;
                            private int b;
                            private String c;

                            @Override
                            public boolean equals(Object other) { /*# not ok #*/
                                Test that = (Test) other; /*# not ok; this may throw ClassCastException #*/
                                return a == that.a;
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsUnsafeCast(problems.next());
        assertEqualsBrokenForNull(problems.next());

        problems.assertExhausted();
    }
}
