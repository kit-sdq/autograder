package de.firemage.autograder.core.check.structure;

import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDuplicateIfBlock extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.DUPLICATE_IF_BLOCK);

    void assertDuplicate(Problem problem, List<String> conditions) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "common-reimplementation",
                Map.of(
                    "suggestion", "if (%s) { ... }".formatted(String.join(" || ", conditions))
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMissingThen() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0);
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfTerminal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                            return;
                        } else {
                            if (i == 1) {
                                System.out.println("zero");
                                return;
                            }
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicate(problems.next(), List.of("i == 0", "i == 1"));

        problems.assertExhausted();
    }


    @Test
    void testIfImplicitElseIf() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                        } else {
                            if (i == 1) {
                                System.out.println("zero");
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
    void testIfElseIfExtraStatement() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                        } else {
                            if (i == 1) {
                                System.out.println("zero");
                            }

                            System.out.println("extra");
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfExtraStatementTerminal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                            return;
                        } else {
                            if (i == 1) {
                                System.out.println("zero");
                                return;
                            }
                            
                            System.out.println("extra");
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicate(problems.next(), List.of("i == 0", "i == 1"));

        problems.assertExhausted();
    }


    @Test
    void testMultipleNestedIf() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                            return;
                        } else {
                            if (i == 1) {
                                System.out.println("zero");
                                return;
                            } else {
                                if (i == 2) {
                                    System.out.println("zero");
                                    return;
                                } else {
                                    if (i == 3) {
                                        System.out.println("zero");
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);
        // The above would be equivalent to a:
        //
        // if (a) {
        //      System.out.println("zero");
        //      return;
        // } else if (b) {
        //      System.out.println("zero");
        //      return;
        // } else if (c) {
        //      System.out.println("zero");
        //      return;
        // } else if (d) {
        //      System.out.println("zero");
        //      return;
        // } else {
        // }
        //
        // Which could be simplified to a single if statement:
        //
        // if (a || b || c || d) {
        //      System.out.println("zero");
        //      return;
        // }
        //
        // The check will not detect this.
        assertDuplicate(problems.next(), List.of("i == 2", "i == 3"));

        problems.assertExhausted();
    }

    @Test
    void testFollowingIfTerminal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                            return;
                        }
                        
                        if (i == 1) {
                            System.out.println("zero");
                            return;
                        }

                        if (i == 2) {
                            System.out.println("zero");
                            return;
                        }
                        
                        if (i == 3) {
                            System.out.println("1");
                            return;
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicate(problems.next(), List.of("i == 0", "i == 1", "i == 2"));

        problems.assertExhausted();
    }

    @Test
    void testFollowingIf() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i == 0) {
                            System.out.println("zero");
                        }
                        
                        if (i == 1) {
                            System.out.println("zero");
                        }

                        if (i == 2) {
                            System.out.println("zero");
                        }

                        if (i == 3) {
                            System.out.println("1");
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfElseTerminal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    boolean foo(int i) {
                        if (i <= 0) {
                            return false;
                        } else if (i + i > 1) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicate(problems.next(), List.of("i <= 0", "i + i > 1"));

        problems.assertExhausted();
    }


    @Test
    void testIfElseIfElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i <= 0) {
                            System.out.println("a");
                        } else {
                            if (i + i > 1) {
                                System.out.println("a");
                            } else {
                                System.out.println("b");
                            }
                        }

                        System.out.println("c");
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicate(problems.next(), List.of("i <= 0", "i + i > 1"));

        problems.assertExhausted();
    }


    @Test
    void testIfElseIfNoElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i <= 0) {
                            System.out.println("a");
                        } else {
                            if (i + i > 1) {
                                System.out.println("a");
                            }
                        }

                        System.out.println("c");
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfElseIfElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    boolean isDoorOpen(int number, boolean[] doors, String[] sweets) {
                        if (number < 1) {
                            return false;
                        } else if (number > sweets.length) {
                            return false;
                        } else if (doors[number - 1]) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicate(problems.next(), List.of("number < 1", "number > sweets.length"));

        problems.assertExhausted();
    }

    @Test
    void testDuplicateNestedIfElseIf() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    void foo(int i) {
                        if (i <= 0) {
                            System.out.println("a");
                        } else {
                            if (i + i > 1) {
                                if (i + 5 > 5) {
                                    System.out.println("a");
                                } else {
                                    System.out.println("b");
                                }
                            } else {
                                System.out.println("c");
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
    void testDuplicateNestedIfElseIfTerminal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                public class Main {
                    int foo(int i) {
                        if (i <= 0) {
                            return 0;
                        } else {
                            if (i + i > 1) {
                                if (i + 5 > 5) {
                                    return 0;
                                } else {
                                    return 1;
                                }
                            } else {
                                return 2;
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
    void testSimilarBranches() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Main",
            """
                record Target(int defense) {
                    public int getDefense() {
                        return this.defense;
                    }
                    
                    public boolean isFaceUp() {
                        return this.getDefense() == 0;
                    }
                    
                    public boolean isBlocking() {
                        return !this.isFaceUp();
                    }
                }
                
                public class Main {
                    private static final int UNKNOWN_PENALTY = 500;

                    int calculate(int unitAtk, Target target) {
                        if (target.isFaceUp()) {
                            return unitAtk - UNKNOWN_PENALTY;
                        } else if (target.isBlocking()) {
                            return unitAtk - target.getDefense();
                        }

                        return unitAtk;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
