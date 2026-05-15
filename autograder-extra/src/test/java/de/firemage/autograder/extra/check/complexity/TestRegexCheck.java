package de.firemage.autograder.extra.check.complexity;

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

class TestRegexCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.COMPLEX_REGEX
    );

    void assertComplex(Problem problem, double score) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "complex-regex", Map.of("score", Double.toString(score), "max", RegexCheck.MAX_ALLOWED_SCORE)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testNotComplex() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.regex.Pattern;

                        public class Example {
                            private String noRegex = "Should we do this? I guess we shouldn't! f*ck you!";
                            private String regex1 = "(foo)* [bar]+ x? x?"; /*# ok #*/
                            private String regex2 = "(?<g1>foo)"; /*# ok #*/
                            private String simpleRegex1 = "\\\\d*.\\\\d*";
                            private String simpleRegex2 = "\\\\d*";
                            private String simpleRegex3 = "^[a-z]+";
                            private String invalidRegex = "(foo* [bar]+ x? x?";

                            private void foo() {
                                Pattern pattern = Pattern.compile(regex1);
                                pattern = Pattern.compile(regex2);
                                pattern = Pattern.compile(simpleRegex1);
                                pattern = Pattern.compile(simpleRegex2);
                                pattern = Pattern.compile(simpleRegex3);
                                pattern = Pattern.compile(invalidRegex);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testComplexWithoutComment() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.regex.Pattern;

                        public class Example {
                            private String regex1 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private String regex2 = "^(?<start>\\\\d+)-->(?<end>\\\\d+):(?<length>\\\\d+)m,(?<type>\\\\d+)x,(?<velocity>\\\\d+)max$"; /*# not ok #*/
                            private String regex3 = "^(?<identifier>\\\\d+),(?<street>\\\\d+),(?<velocity>\\\\d+),(?<acceleration>\\\\d+)$"; /*# not ok #*/

                            private void foo() {
                                Pattern pattern = Pattern.compile(regex1);
                                pattern = Pattern.compile(regex2);
                                pattern = Pattern.compile(regex3);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertComplex(problems.next(), 737.34);
        assertComplex(problems.next(), 191.891);
        assertComplex(problems.next(), 154.112);

        problems.assertExhausted();
    }

    @Test
    void testComplexWithComment() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.regex.Pattern;

                        public class Example {
                            /**
                             * This comment is explaining how the regex works...
                             */
                            private static final String COMPLICATED_REGEX_1 = "^(?<identifier>\\\\d+),(?<street>\\\\d+),(?<velocity>\\\\d+),(?<acceleration>\\\\d+)$";
                            // Inline comments should be acceptable as well
                            private static final String COMPLICATED_REGEX_2 = "^(?<identifier>\\\\d+),(?<street>\\\\d+),(?<velocity>\\\\d+),(?<acceleration>\\\\d+)$";

                            private void foo() {
                                Pattern pattern = Pattern.compile(COMPLICATED_REGEX_1);
                                pattern = Pattern.compile(COMPLICATED_REGEX_2);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testFormatString() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.regex.Pattern;

                        public class Example {
                            private static final String FORMAT_STRING_1 = "coordinate (%s, %s) is invalid!";
                            private static final String FORMAT_STRING_2 = "coordinate (%s, %s)\\n is invalid?\\n";
                            
                            private void foo() {
                                Pattern pattern = Pattern.compile(FORMAT_STRING_1);
                                pattern = Pattern.compile(FORMAT_STRING_2);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testContext() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.regex.Pattern;

                        public class Example {
                            private static final String DEFINITELY_REGEX_1 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private static final String DEFINITELY_REGEX_2 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private static final String DEFINITELY_REGEX_3 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private static final String DEFINITELY_REGEX_4 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private static final String DEFINITELY_REGEX_5 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private static final String DEFINITELY_REGEX_6 = "^[a-z]+(?: \\\\S+)?$"; /*# not ok #*/
                            private static final String UNUSED_REGEX = "^[a-z]+(?: \\\\S+)?$"; /*# ok #*/
                            private static final String NOT_USED_AS_REGEX = "^[a-z]+(?: \\\\S+)?$"; /*# ok #*/

                            private static final Pattern SYMBOL_REGEX = Pattern.compile("[0-9a-zA-Z]*"); /*# ok #*/

                            void foo() {
                                boolean matches = Pattern.matches(DEFINITELY_REGEX_1, "foo bar x");
                                matches = "foo bar x".matches(DEFINITELY_REGEX_2);
                                String f = "foo bar x".replaceAll(DEFINITELY_REGEX_3, "foo bar x");
                                f = "foo bar x".replaceFirst(DEFINITELY_REGEX_4, "foo bar x");
                                String[] parts = "foo bar x".split(DEFINITELY_REGEX_5);
                                parts = "foo bar x".split(DEFINITELY_REGEX_6, -1);

                                System.out.println(NOT_USED_AS_REGEX);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertComplex(problems.next(), 737.34);
        assertComplex(problems.next(), 737.34);
        assertComplex(problems.next(), 737.34);
        assertComplex(problems.next(), 737.34);
        assertComplex(problems.next(), 737.34);
        assertComplex(problems.next(), 737.34);

        problems.assertExhausted();
    }

    @Test
    void testReproduceCrash() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "B",
                    """
                        public class B {
                            B(String string) {}
                        }
                        """
                ),
                Map.entry(
                    "A",
                    """
                        public class A extends B {
                            A() {
                                super("abc123"); // did result in crash
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
