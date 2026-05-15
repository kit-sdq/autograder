package de.firemage.autograder.core.check.general;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLoopShouldBeWhile extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.LOOP_SHOULD_BE_WHILE);

    private static <T> T debugPrint(T value) {
        System.out.println("\"%s\"".formatted(value.toString().replace("\n", "\\n").replace("\r", "\\r")));
        return value;
    }

    void assertEqualsWhile(Problem problem, String condition, String body) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "loop-should-be-while",
                Map.of("suggestion", """
                            %nwhile (%s) %s""".formatted(condition, body))
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testForWithUsedCounter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        for (int i = 0; i < array.length; i++) {
                            System.out.println("i: " + i + " array[i]: " + array[i]);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testForWithoutInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        for (; i < array.length; i++) {
                            System.out.println("i: " + i + " array[i]: " + array[i]);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testForWithoutInitAndUpdate() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        int i = 0;
                        for (; i < array.length;) {
                            System.out.println("i: " + i + " array[i]: " + array[i]);
                            i++;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsWhile(
            problems.next(),
            "i < array.length",
            "{%n    System.out.println(\"i: \" + i + \" array[i]: \" + array[i]);%n    i++;%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    void testEndlessFor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(String[] array) {
                        for (;;) {
                            System.out.println("hello");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    @Disabled("Might be implemented in the future")
    void testUnusedCounter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(int count) {
                        String result = "";
                        for (int i = 0; result.length() < count; i++) {
                            result += "a";
                        }
                        System.out.println(result);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsWhile(
            problems.next(),
            "result.length() < count",
            "{%n    result += \"a\";%n}".formatted()
        );

        problems.assertExhausted();
    }

    @Test
    @Disabled("Might be implemented in the future")
    void testUnusedCounterExternalInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    void test(int count) {
                        String result = "";
                        int i = 0;
                        for (; result.length() < count; i++) {
                            result += "a";
                        }
                        System.out.println(result);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsWhile(
            problems.next(),
            "result.length() < count",
            "{%n    result += \"a\";%n}".formatted()
        );

        problems.assertExhausted();
    }
}


