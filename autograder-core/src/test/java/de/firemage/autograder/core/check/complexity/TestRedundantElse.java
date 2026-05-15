package de.firemage.autograder.core.check.complexity;

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

class TestRedundantElse extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REDUNDANT_ELSE);

    void assertRedundantElse(Problem problem, String value) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "redundant-else",
                Map.of(
                    "expected", "if (a) { ... %s; } elseCode;".formatted(value),
                    "given", "if (a) { ... %s; } else { elseCode; }".formatted(value)
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertRedundantElseIf(Problem problem, String value) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "redundant-else",
                Map.of(
                    "expected", "if (a) { ... %s; } else if (b) { ... } elseCode;".formatted(value),
                    "given", "if (a) { ... %s; } else if (b) { ... } else { elseCode; }".formatted(value)
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testIfElseReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    } else {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 2;
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        assertRedundantElse(problems.next(), "return 1");

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    } else if (a == 1) {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 2;
                    }
                    
                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    return 0;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfElseReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    } else if (a == 1) {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 2;
                    } else {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 3;
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        assertRedundantElseIf(problems.next(), "return 1");

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfElseIfElseReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    } else if (a == 1) {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 2;
                    } else if (a == 2) {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 2;
                    }  else {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 3;
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testIfElseNestedIfElseReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    } else {
                        if (a == 1) {
                            System.out.println("hello");
                            return 2;
                        } else {
                            return 3;
                        }
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfElseIfElseNonTerminalElseIfReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    } else if (a == 1) {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                    } else {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                    }
                    
                    return 3;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0) {
                        return 1;
                    }

                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    System.out.println("hello");
                    return 0;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIfWithoutBlockReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                int foo(int a) {
                    if (a == 0); {
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        System.out.println("hello");
                        return 1;
                    }
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


