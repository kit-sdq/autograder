package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestRedundantIfForBooleanCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REDUNDANT_IF_FOR_BOOLEAN);

    void assertEqualsRedundant(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "common-reimplementation",
                Map.of("suggestion", suggestion)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testAssignment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public void doA(int a) {
                        boolean b = false;

                        if (a == 0) {
                            b = true;
                        } else {
                            b = false;
                        }
                    }

                    public void doB(int a) {
                        boolean b = false;

                        if (a == 1) {
                            b = false;
                        } else {
                            b = true;
                        }
                    }

                    public void doC(int a) {
                        boolean b = false;

                        if (!(a == 2)) {
                            b = true;
                        } else {
                            b = false;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "b = a == 0");
        assertEqualsRedundant(problems.next(), "b = a != 1");
        assertEqualsRedundant(problems.next(), "b = !(a == 2)");

        problems.assertExhausted();
    }

    // if (a) { return true; } else { return false; }
    // if (!a) { return true; } else { return false; }
    @Test
    void testReturnIfElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean doA(int a) {
                        if (a == 0) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    public boolean doB(int a) {
                        if (a == 1) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    public boolean doC(int a) {
                        if (!(a == 2)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "return a == 0");
        assertEqualsRedundant(problems.next(), "return a != 1");
        assertEqualsRedundant(problems.next(), "return !(a == 2)");

        problems.assertExhausted();
    }

    // if (a) { return true; } return false;
    // if (!a) { return true; } return false;
    @Test
    void testReturnIfImplicitElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean doA(int a) {
                        if (a == 0) {
                            return true;
                        }

                        return false;
                    }
                    
                    public boolean doB(int a) {
                        if (a == 1) {
                            return false;
                        }

                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "return a == 0");
        assertEqualsRedundant(problems.next(), "return a != 1");

        problems.assertExhausted();
    }

    // if (a) { return true; } else if (b) { return true; } else { return false; }
    @Test
    void testReturnIfElseIfElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean doA(int a) {
                        if (a == 0) {
                            return true;
                        } else if (a == 1) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    
                    public boolean doB(int a) {
                        if (a == 2) {
                            return true;
                        } else if (a == 3) {
                            return true;
                        }

                        return false;
                    }
                    
                    public boolean doC(int a) {
                        if (a == 4) {
                            return true;
                        } else if (a == 5) {
                            return false;
                        }

                        return false;
                    }
                }
                """
        ), PROBLEM_TYPES);

        // NOTE: it ignores the first if, only the else if else are checked
        assertEqualsRedundant(problems.next(), "return a == 1");
        // assertEqualsRedundant(problems.next(), "return a == 3");

        problems.assertExhausted();
    }

    @Test
    void testIfWithLargeConditions() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    boolean test(boolean a, boolean b, boolean c, boolean d, boolean e, boolean f) {
                        if (a && b && !c) {
                            return true;
                        } else {
                            return e || f;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "return a && b && !c || (e || f)");

        problems.assertExhausted();
    }

    @Test
    void testIfWithExtraStatement() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean doA(int a) {
                        if (a == 0) { //# ok
                            System.out.println("a == 0");
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @ParameterizedTest
    @CsvSource(
        delimiter = ';',
        useHeadersInDisplayName = true,
        // the condition for the then is always "isSafe"
        value = {
            " Then            ; Else              ; Suggestion                                       ",
            // all possible combinations of conditions where they are partially literals:
            " isEdgeLegal     ; true              ; !isSafe || isEdgeLegal                           ",
            " isEdgeLegal     ; false             ; isSafe && isEdgeLegal                            ",
            " true            ; isEdgeSafe        ; isSafe || isEdgeSafe                             ",
            " false           ; isEdgeSafe        ; !isSafe && isEdgeSafe                            ",
            // all possible combinations where the then and else are both literals
            // " true            ; true              ; true                                             ",
            " true            ; false             ; isSafe                                           ",
            " false           ; true              ; !isSafe                                          ",
            // " false           ; false             ; false                                            ",
            // all possible combinations where the then and else are both non-literals
            // " isEdgeLegal     ; isEdgeSafe        ; isSafe && isEdgeLegal || !isSafe && isEdgeSafe   ",
        }
    )
    void testIfElseReturn(String thenValue, String elseValue, String suggestion) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
            public class Test {
                boolean testA(boolean isSafe, boolean isEdgeLegal, boolean isEdgeSafe) {
                    if (isSafe) {
                        return %s;
                    }

                    return %s;
                }
                
                boolean testB(boolean isSafe, boolean isEdgeLegal, boolean isEdgeSafe) {
                    if (isSafe) {
                        return %s;
                    } else {
                        return %s;
                    }
                }
            }""".formatted(thenValue, elseValue, thenValue, elseValue)
        ), PROBLEM_TYPES);

        if (suggestion != null) {
            assertEqualsRedundant(problems.next(), "return %s".formatted(suggestion));
            assertEqualsRedundant(problems.next(), "return %s".formatted(suggestion));
        }

        problems.assertExhausted();
    }

    @Test
    void testInEquals() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    int a;

                    @Override
                    public boolean equals(Object other) {
                        if (a == 0) {
                            return a == 1;
                        }

                        return a == 2;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testIgnoreInvalidIfWithValidElseIfElse() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean doA(int a) {
                        if (a == 0) {
                            // because of the print, one can not simply remove the if
                            System.out.println("a == 0");
                            return true;
                        } else if (a == 1) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        // the else-if-else can be replaced with a return, because the if is terminated by a return
        assertEqualsRedundant(problems.next(), "return a == 1");
        problems.assertExhausted();
    }

    @Test
    void testIfWithoutBody() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean doA(int a) {
                        if (a == 0);

                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMissingParensAroundStringConcatSuggestion() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public boolean isRotation(String word, String rotation) {
                        if (word.length() != rotation.length()) {
                            return false;
                        }

                        return (word + word).contains(rotation);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "return word.length() == rotation.length() && (word + word).contains(rotation)");

        problems.assertExhausted();
    }


    @Test
    void testEqualWithGeneric() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    private String patternComparator;

                    private <S> boolean isInvalidPrecondition(S value) {
                        if (value == null) {
                            return true;
                        }
                        return value == patternComparator;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "return value == null || value == patternComparator");

        problems.assertExhausted();
    }
}
