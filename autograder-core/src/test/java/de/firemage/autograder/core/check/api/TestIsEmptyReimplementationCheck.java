package de.firemage.autograder.core.check.api;

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

class TestIsEmptyReimplementationCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.STRING_IS_EMPTY_REIMPLEMENTED,
        ProblemType.COLLECTION_IS_EMPTY_REIMPLEMENTED
    );

    void assertEqualsIsEmpty(String original, String suggestion, Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "suggest-replacement",
                Map.of("original", original, "suggestion", suggestion)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testStringIsEmpty() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        String foo = "";

                        //# ok
                        var isTrue = foo.isEmpty();
                        var isBlank = foo.isBlank();

                        //# not ok
                        var isEmpty = foo.equals("");
                        var k = "".equals("hello");

                        //# ok
                        foo.equals("foo");
                        foo.equals(null);

                        //# not ok
                        var a = foo.length() == 0;
                        var d = foo.length() >= 1;
                        var f = foo.length() > 0;
                        var h = 0 < foo.length();
                        var j = 1 <= foo.length();

                        //# ok
                        var b = foo.length() == 1;
                        var c = foo.length() >= 0;
                        var e = foo.length() > 1;
                        var g = 1 < foo.length();
                        var i = 0 <= foo.length();
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsIsEmpty("foo.equals(\"\")", "foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("\"\".equals(\"hello\")", "\"hello\".isEmpty()", problems.next());
        assertEqualsIsEmpty("foo.length() == 0", "foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("foo.length() >= 1", "!foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("foo.length() > 0", "!foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("0 < foo.length()", "!foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("1 <= foo.length()", "!foo.isEmpty()", problems.next());
        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression          | Arguments          | Expected     ",
            " v.size() < 0        | List<String> v     |              ",
            " v.size() > 1        | List<String> v     |              ",
            " v.size() <= 0       | List<String> v     | v.isEmpty()  ",
            " v.size() <= 1       | List<String> v     |              ",
            //
            " v.size() > 0        | List<String> v     | !v.isEmpty() ",
            " v.size() > 1        | List<String> v     |              ",
            " v.size() >= 0       | List<String> v     |              ",
            " v.size() >= 1       | List<String> v     | !v.isEmpty() ",
            //
            " v.size() != 0       | List<String> v     | !v.isEmpty() ",
            " v.size() != 1       | List<String> v     |              ",
            " v.size() == 0       | List<String> v     | v.isEmpty()  ",
            " v.size() == 1       | List<String> v     |              ",
            //
            " 0 < v.size()        | List<String> v     | !v.isEmpty() ",
            " 1 > v.size()        | List<String> v     | v.isEmpty()  ",
            " 0 <= v.size()       | List<String> v     |              ",
            " 1 <= v.size()       | List<String> v     | !v.isEmpty() ",
            //
            " 0 > v.size()        | List<String> v     |              ",
            " 1 > v.size()        | List<String> v     | v.isEmpty()  ",
            " 0 >= v.size()       | List<String> v     | v.isEmpty()  ",
            " 1 >= v.size()       | List<String> v     |              ",
            //
            " 0 != v.size()       | List<String> v     | !v.isEmpty() ",
            " 1 != v.size()       | List<String> v     |              ",
            " 0 == v.size()       | List<String> v     | v.isEmpty()  ",
            " 1 == v.size()       | List<String> v     |              ",
        }
    )
    void testIsEmptyNotString(String expression, String arguments, String expected) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            "import java.util.List; public class Test { public void foo(%s) { System.out.println(%s); } }".formatted(
                arguments,
                expression
            )
        ), PROBLEM_TYPES);

        if (expected != null) {
            assertEqualsIsEmpty(expression, expected, problems.next());
        }

        problems.assertExhausted();
    }

    @Test
    void testInlining() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final String EMPTY_STRING = "";
                    private static final int ONE = 1;
                    private static final int ZERO = 0;

                    public static void main(String[] args) {
                        String foo = "";

                        //# not ok
                        var a = foo.equals(EMPTY_STRING);
                        var b = foo.length() > ZERO;
                        var c = foo.length() < ONE;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsIsEmpty("foo.equals(EMPTY_STRING)", "foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("foo.length() > ZERO", "!foo.isEmpty()", problems.next());
        assertEqualsIsEmpty("foo.length() < ONE", "foo.isEmpty()", problems.next());

        problems.assertExhausted();
    }
}


