package de.firemage.autograder.core.check.complexity;
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

class TestChainedIfCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.MERGE_NESTED_IF, ProblemType.UNMERGED_ELSE_IF);

    private void assertEqualsNested(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "merge-nested-if",
                    Map.of("suggestion", suggestion)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    private void assertEqualsElseIf(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage("common-reimplementation", Map.of(
                    "suggestion", "} else if (%s) { ... }".formatted(suggestion)
                ))
            ),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testNestedIfsWithImplicitElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        if (true) {
                            if (false) {
                            }
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsNested(problems.next(), "true && false");
        problems.assertExhausted();
    }

    @Test
    void testNestedIfsWithOuterElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        if (true) {
                            if (false) {
                                System.out.println("A");
                            }
                        } else {
                            System.out.println("B");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testElseIfWithNestedIfNoElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x, int y) {
                        if (x == 0) {
                            System.out.println("A");
                        } else if (x == 1) {
                            if (y == 0) {
                                System.out.println("B");
                            }
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsNested(problems.next(), "x == 1 && y == 0");

        problems.assertExhausted();
    }

    @Test
    void testElseIfWithNestedIfWithElse() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x, int y) {
                        if (x == 0) {
                            System.out.println("A");
                        } else if (x == 1) {
                            if (y == 0) {
                                System.out.println("B");
                            }
                        } else {
                            System.out.println("C");
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRegularIfElseIfElseChain() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x, int y) {
                        if (x == 0) {
                            System.out.println("A");
                        } else if (x == 1) {
                            System.out.println("B");
                        } else {
                            System.out.println("C");
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);
        problems.assertExhausted();
    }

    @Test
    void testElseWithNestedIfAndCode() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x, int y) {
                        if (x == 0) {
                        } else if (x == 1) {
                        } else {
                            System.out.println("A");
                            if (x == 2) {
                                System.out.println("B");
                            }
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testElseWithNestedIf() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x, int y) {
                        if (x == 0) {
                        } else if (x == 1) {
                        } else {
                            if (x == 2) {
                                System.out.println("B");
                            }
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsElseIf(problems.next(), "x == 2");

        problems.assertExhausted();
    }

    @Test
    void testCombineNestedIf() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x) {
                        if (x > 0) {
                            if (x < 10) {
                                System.out.println("A");
                            }
                        }
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsNested(problems.next(), "x > 0 && x < 10");

        problems.assertExhausted();
    }

    @Test
    void testNestedIfWithReturn() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int a) {
                        if (a <= 0) {
                            if (a == -3) {
                                System.out.println("a is -3");
                            }
                            return;
                        }
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testComplicatedIfWithNested() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int a, int b) {
                        if (a == 1) {
                            if (b == 2) {
                                System.out.println("a is 1");
                            }
                        } else if (a == 2) {
                            if (b == 3) {
                                throw new IllegalStateException("an error occurred");
                            }
                            System.out.println("a is 2");
                        } else if (a == 3) {
                            if (b == 3) {
                                System.out.println("a is 3 and b is 3");
                            } else {
                                throw new IllegalStateException("should never happen");
                            }
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testCombineNestedIfWithCode() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x) {
                        if (x > 0) {
                            System.out.println("A");
                            if (x < 10) {
                                System.out.println("B");
                            }
                        }
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testCombineNestedIfTrailingCode() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x) {
                        if (x > 0) {
                            if (x < 10) {
                                System.out.println("B");
                            }
                            System.out.println("A");
                        }
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testIfWithoutBlock() throws LinterException, IOException {
        // this resulted in a crash in the past
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void callable(int x) {
                        if (x > 0);
                    }
                
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


