package de.firemage.autograder.core.check.general;

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

class TestLoopShouldBeDoWhile extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.LOOP_SHOULD_BE_DO_WHILE);

    private static <T> T debugPrint(T value) {
        System.out.println("\"%s\"".formatted(value.toString().replace("\n", "\\n").replace("\r", "\\r")));
        return value;
    }

    void assertEqualsDoWhile(Problem problem, String body, String condition) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "loop-should-be-do-while",
                Map.of("suggestion", """
                            %ndo %s while (%s)""".formatted(body, condition))
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testNoExtraStatements() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("a");
                        System.out.println("b");
                        System.out.println("c");
                        while (condition) {
                            System.out.println("a");
                            System.out.println("b");
                            System.out.println("c");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsDoWhile(
            problems.next(),
            "{%n    System.out.println(\"a\");%n    System.out.println(\"b\");%n    System.out.println(\"c\");%n}".formatted(),
            "condition"
        );

        problems.assertExhausted();
    }

    @Test
    void testExtraBefore() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("a");
                        System.out.println("b");
                        System.out.println("c");
                        while (condition) {
                            System.out.println("b");
                            System.out.println("c");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsDoWhile(
            problems.next(),
            "{%n    System.out.println(\"b\");%n    System.out.println(\"c\");%n}".formatted(),
            "condition"
        );

        problems.assertExhausted();
    }

    @Test
    void testExtraAfter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("b");
                        System.out.println("c");
                        while (condition) {
                            System.out.println("b");
                            System.out.println("c");
                        }
                        System.out.println("d");
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsDoWhile(
            problems.next(),
            "{%n    System.out.println(\"b\");%n    System.out.println(\"c\");%n}".formatted(),
            "condition"
        );

        problems.assertExhausted();
    }

    @Test
    void testExtraInLoopStart() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("b");
                        System.out.println("c");
                        while (condition) {
                            System.out.println("a");
                            System.out.println("b");
                            System.out.println("c");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testExtraInLoopEnd() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("a");
                        System.out.println("b");
                        while (condition) {
                            System.out.println("a");
                            System.out.println("b");
                            System.out.println("c");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testEmptyLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("a");
                        System.out.println("b");
                        while (condition) {}
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testNoPrecedingStatements() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        while (condition) {
                            System.out.println("a");
                            System.out.println("b");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSingleStatement() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        int i = 0;
                        i += 1;
                        while (condition) i += 1;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsDoWhile(
            problems.next(),
            "{%n    i += 1;%n}".formatted(),
            "condition"
        );

        problems.assertExhausted();
    }

    @Test
    void testNoStatement() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        int i = 0;
                        i += 1;
                        while (condition);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    // TODO: test truncation
    // TODO: test with double or triple indent in source
    @Test
    void testTruncation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition) {
                        System.out.println("a");
                        System.out.println("b");
                        System.out.println("c");
                        System.out.println("d");
                        System.out.println("e");
                        System.out.println("f");
                        System.out.println("g");
                        while (condition) {
                            System.out.println("a");
                            System.out.println("b");
                            System.out.println("c");
                            System.out.println("d");
                            System.out.println("e");
                            System.out.println("f");
                            System.out.println("g");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsDoWhile(
            problems.next(),
            "{%n    System.out.println(\"a\");%n    System.out.println(\"b\");%n    System.out.println(\"c\");%n    System.out.println(\"d\");%n    System.out.println(\"e\");%n    ...%n}".formatted(),
            "condition"
        );

        problems.assertExhausted();
    }

    @Test
    void testDeepIndentBody() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(boolean condition, int count) {
                        if (count > 0) {
                            if (count > 1) {
                                if (count > 2) {
                                    System.out.println("a");
                                    System.out.println("b");
                                    System.out.println("c");
                                    while (condition) {
                                        System.out.println("a");
                                        System.out.println("b");
                                        System.out.println("c");
                                    }
                                }
                            }
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsDoWhile(
            problems.next(),
            "{%n    System.out.println(\"a\");%n    System.out.println(\"b\");%n    System.out.println(\"c\");%n}".formatted(),
            "condition"
        );

        problems.assertExhausted();
    }
}


