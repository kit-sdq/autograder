package de.firemage.autograder.core.check.comment;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestJavadocStubCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.JAVADOC_STUB_DESCRIPTION, ProblemType.JAVADOC_STUB_RETURN_TAG,
        ProblemType.JAVADOC_STUB_THROWS_TAG, ProblemType.JAVADOC_STUB_PARAMETER_TAG
    );

    void assertEqualsStubDescription(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("javadoc-stub-description")),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsStubTag(Problem problem, String tag) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "javadoc-stub-tag",
                Map.of("tag", tag)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    private static String makeJavadoc(String... entries) {
        return "/**%s%n */".formatted(
            Arrays.stream(entries).map("\n * "::concat).collect(Collectors.joining())
        );
    }


    @Test
    void testJavadocStubDescription() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    /**
                     *
                     */
                    String field;

                    /**
                     *
                     * @param a this value must not be null
                     * @return the passed value
                     * @throws Exception if it failed to test the value
                     */
                    public String test(String a) throws Exception {
                        return a;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsStubDescription(problems.next());
        assertEqualsStubDescription(problems.next());

        problems.assertExhausted();
    }


    @Test
    void testJavadocStubTag() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                /**
                 * This is a test class.
                 *
                 * @author
                 * @since
                 */
                public class Test {
                    String field;

                    /**
                     * Tests a value.
                     *
                     * @param a the string value
                     * @return the string value
                     * @throws Exception
                     */
                    public String test(String a) throws Exception {
                        return a;
                    }
                }
                """
        ), PROBLEM_TYPES);

        //assertEqualsStubTag(problems.next(), "author");
        //assertEqualsStubTag(problems.next(), "since");
        assertEqualsStubTag(problems.next(), "@param a the string value");
        assertEqualsStubTag(problems.next(), "@return the string value");
        assertEqualsStubTag(problems.next(), "@throws Exception");

        problems.assertExhausted();
    }



    @Test
    void testJavadocStubInInherited() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    /**
                     *
                     * @param other
                     * @return
                     */
                    public boolean equals(Object other) {
                        return this == other;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


