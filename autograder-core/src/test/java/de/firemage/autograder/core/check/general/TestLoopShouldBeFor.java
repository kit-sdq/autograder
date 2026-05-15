package de.firemage.autograder.core.check.general;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLoopShouldBeFor extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.LOOP_SHOULD_BE_FOR);

    private static <T> T debugPrint(T value) {
        System.out.println("\"%s\"".formatted(value.toString().replace("\n", "\\n").replace("\r", "\\r")));
        return value;
    }

    void assertEqualsFor(Problem problem, String init, String condition, String update, String body) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "loop-should-be-for",
                Map.of("suggestion", """
                            %nfor (%s; %s; %s) %s""".formatted(init, condition, update, body))
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsFor(Problem problem, String init, String condition, String update, String beforeLoop, String body) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "loop-should-be-for",
                Map.of("suggestion", """
                            %n%s%nfor (%s; %s; %s) %s""".formatted(beforeLoop, init, condition, update, body))
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testCounterOnlyUsedInLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            System.out.println("i: " + i + " array[i]: " + array[i]);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "i < array.length",
            "i += 1",
            "{%n    System.out.println(\"i: \" + i + \" array[i]: \" + array[i]);%n}".formatted()
        );

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Update              | Expected     ",
            " i += 1              | 1            ",
            " i += 2              | 1            ",
            " i -= 2              | 1            ",
            " i *= 2              | 1            ",
            " i /= 2              | 1            ",
            // test unary operators:
            " i++                 | 1            ",
            " i--                 | 1            ",
            // test simple assignments:
            " i = i + 1           | 1            ",
            " i = i - 1           | 1            ",
            " i = i * 2           | 1            ",
            " i = i / 2           | 1            ",
            " i = i % 2           | 1            ",
            " i = 1               | 1            ",
        }
    )
    void testDifferentAssignments(String update, String expected) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            System.out.println("i: " + i + " array[i]: " + array[i]);
                            %s;
                        }
                    }
                }
                """.formatted(update)
        ), PROBLEM_TYPES);

        if (expected != null) {
            assertEqualsFor(
                problems.next(),
                "int i = 0",
                "i < array.length",
                update,
                "{%n    System.out.println(\"i: \" + i + \" array[i]: \" + array[i]);%n}".formatted()
            );
        }


        problems.assertExhausted();
    }

    @Test
    void testNonNumericCounter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        char i = 'a';
                        while ((int) i < array.length) {
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultipleCounterUpdates() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            i += 2;
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUpdateNotLastStatementButLastUse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            System.out.println("i: " + i);
                            i += 1;
                            System.out.println("a");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "i < array.length",
            "i += 1",
            "{%n    System.out.println(\"i: \" + i);%n    System.out.println(\"a\");%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testSingleStatementBlock() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "i < array.length",
            "i += 1",
            "{%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testSingleStatement() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) i += 1;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "i < array.length",
            "i += 1",
            "{%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testCounterDeclaredWithOtherVariablesBeforeLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    String test(String sentence) {
                        int c = 0;
                        boolean spaceBefore = true;
                        String word = "";
                        while (c < sentence.length()) {
                            if (Character.isWhitespace(sentence.charAt(c))) {
                                spaceBefore = true;
                            } else if (spaceBefore) {
                                word += sentence.charAt(c);
                                spaceBefore = false;
                            }
                            c++;
                        }
                        return word;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int c = 0",
            "c < sentence.length()",
            "c++",
            "{%n    if (Character.isWhitespace(sentence.charAt(c))) {%n        spaceBefore = true;%n    } else if (spaceBefore) {%n        word += sentence.charAt(c);%n        spaceBefore = false;%n    }%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testMissingUpdate() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            System.out.println("i: " + i);
                            System.out.println("a");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMissingLocalVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(int i, String[] array) {
                        while (i < array.length) {
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testLocalVariableNotFirstStatementBeforeLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        System.out.println(i);
                        i += 1;

                        while (i < array.length) {
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testForEachOnlyCounting() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        for (String value : array) {
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "i < array.length",
            "i += 1",
            "{%n    System.out.println(\"i: \" + i);%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testForEachList() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                class Test {
                    void test(List<String> list) {
                        int i = 0;
                        for (String value : list) {
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "i < list.size()",
            "i += 1",
            "{%n    System.out.println(\"i: \" + i);%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testForEachOnlyCountingUsedAfterLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        for (String value : array) {
                            System.out.println("i: " + i);
                            i += 1;
                        }
                        System.out.println("i: " + i);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "",
            "i < array.length",
            "i += 1",
            "int i = 0",
            "{%n    System.out.println(\"i: \" + i);%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testForEachVariableUsed() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        for (String value : array) {
                            System.out.println("i: " + i + " value: " + value);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUpdateNotLastStatementAndUsed() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            System.out.println("i: " + i);
                            i += 1;
                            System.out.println("a" + i);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testCounterNotUsedInCondition() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (true) {
                            if (i >= array.length) {
                                break;
                            }
                            System.out.println("i: " + i);
                            i += 1;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "int i = 0",
            "",
            "i += 1",
            "{%n    if (i >= array.length) {%n        break;%n    }%n    System.out.println(\"i: \" + i);%n}".formatted()
        );

        problems.assertExhausted();
    }


    @Test
    void testCounterUsedAfterLoop() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        while (i < array.length) {
                            System.out.println("i: " + i + " array[i]: " + array[i]);
                            i += 1;
                        }
                        System.out.println(i);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsFor(
            problems.next(),
            "",
            "i < array.length",
            "i += 1",
            "int i = 0",
            "{%n    System.out.println(\"i: \" + i + \" array[i]: \" + array[i]);%n}".formatted()
        );

        problems.assertExhausted();
    }
}


