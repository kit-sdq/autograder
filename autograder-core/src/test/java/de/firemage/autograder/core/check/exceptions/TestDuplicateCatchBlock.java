package de.firemage.autograder.core.check.exceptions;
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

class TestDuplicateCatchBlock extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.DUPLICATE_CATCH_BLOCK);

    void assertDuplicateCatch(Problem problem, List<String> exceptions, String variable) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "common-reimplementation", Map.of(
                "suggestion", "try { ... } catch (%s %s) { ... }".formatted(
                    String.join(" | ", exceptions), variable)
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMotivation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "IllegalPlayerNameException",
                    """
                    public class IllegalPlayerNameException extends RuntimeException {
                        public IllegalPlayerNameException(String message) {
                            super(message);
                        }
                    }
                    """
                ),
                Map.entry(
                    "IllegalGodsFavorException",
                    """
                    public class IllegalGodsFavorException extends RuntimeException {
                        public IllegalGodsFavorException(String message) {
                            super(message);
                        }
                    }
                    """
                ),
                Map.entry(
                    "IllegalHealthPointsException",
                    """
                    public class IllegalHealthPointsException extends Exception {
                        public IllegalHealthPointsException(String message) {
                            super(message);
                        }
                    }
                    """
                ),
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            private static void foo(int count) throws IllegalHealthPointsException {
                                if (count == 1) {
                                    throw new IllegalPlayerNameException("Player name is illegal");
                                } else if (count == 2) {
                                    throw new IllegalGodsFavorException("God's favor is illegal");
                                } else if (count == 3) {
                                    throw new IllegalHealthPointsException("Health points are illegal");
                                }
                                
                                System.out.println("Success");
                            }
                        
                            public static void main(String[] args) {
                                try {
                                    foo(1);
                                } catch (IllegalPlayerNameException e) {
                                    e.printStackTrace();
                                } catch (IllegalGodsFavorException e) {
                                    e.printStackTrace();
                                } catch (IllegalHealthPointsException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertDuplicateCatch(
            problems.next(),
            List.of("IllegalPlayerNameException", "IllegalGodsFavorException", "IllegalHealthPointsException"),
            "e"
        );

        problems.assertExhausted();
    }

    @Test
    void testAccessesDifferentVariables() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    int a = 1;
                    int b = 2;

                    try {
                        foo(1);
                    } catch (IllegalArgumentException e) {
                        System.out.println(a);
                    } catch (IllegalStateException e) {
                        System.out.println(b);
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testDifferentCatchBlocks() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try {
                        foo(1);
                    } catch (IllegalArgumentException e) {
                        System.out.println(e.getMessage());
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSingleCatchBlock() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try {
                        foo(1);
                    } catch (IllegalArgumentException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMergeMultiTypeBlock() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try {
                        foo(1);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        assertDuplicateCatch(
            problems.next(),
            List.of("IllegalArgumentException", "IllegalStateException", "NullPointerException"),
            "e"
        );

        problems.assertExhausted();
    }

    @Test
    void testDifferingVariableNames() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try {
                        foo(1);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (NullPointerException u) {
                        u.printStackTrace();
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        assertDuplicateCatch(
            problems.next(),
            List.of("IllegalArgumentException", "NullPointerException"),
            "e"
        );

        problems.assertExhausted();
    }

    @Test
    void testTryWithoutCatch() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            import java.util.Scanner;

            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try (Scanner scanner = new Scanner(System.in)){
                        foo(1);
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMergeNotFirstCatch() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try {
                        foo(1);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        System.out.println("Error");
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        assertDuplicateCatch(
            problems.next(),
            List.of("NullPointerException", "IllegalStateException"),
            "e"
        );

        problems.assertExhausted();
    }

    @Test
    void testCodeDuplicate() throws IOException, LinterException {
        // this tests that the DuplicateCatchBlock check does not report the same problems that DuplicateCode reports

        String largeDuplicateCode = "System.out.println(\"1\");%n".formatted().repeat(30);
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
            public class Main {
                private static void foo(int count) {
                    System.out.println("Success" + count);
                }

                public static void main(String[] args) {
                    try {
                        foo(1);
                    } catch (IllegalArgumentException e) {
                        %s
                    } catch (NullPointerException e) {
                        %s
                    }
                }
            }
            """.formatted(largeDuplicateCode, largeDuplicateCode)
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

}


