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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestTryCatchComplexity extends AbstractCheckTest {
    void assertEqualsTryCatchComplexity(Problem problem) {
        assertEquals(ProblemType.TRY_CATCH_COMPLEXITY, problem.getProblemType());
        assertEquals(
                this.linter.translateMessage(new LocalizedMessage(
                TryCatchComplexity.LOCALIZED_MESSAGE_KEY,
                Map.of("max", TryCatchComplexity.MAX_ALLOWED_STATEMENTS))),
                this.linter.translateMessage(problem.getExplanation()));
    }
    @Test
    void testBlockIsNotCounted() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                   int a = 1;
                                   int b = 2;
                                   int c = 3;
                                   int d = 4;
                                   int e = 5;
                                   int f = 6;
                                   int g = 7;
                                   int h = 8;
                                   int i = 9;
                                   int j = 10;
                                   int k = 11;
                                   int l = 12;
                                   int m = 13;
                                   int n = 14;
                                   {
                                       System.out.println(a + b + c + d + e + f +
                                            g + h + i + j + k + l + m + n);
                                   }
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }

                            }
                        }
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        problems.assertExhausted();
    }
    @Test
    void testNestedFor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                    int a = 5;
                                    if (a == 5) {
                                        System.out.println("Hello");
                                        for (int i = 0; i < 10; i++) {
                                            int c = 5;
                                            int f = c + 6;
                                            System.out.println(f);
                                        }
                                    }
                                    if (a != 5);
                                    for (int i = 0; i < 2; i++) {
                                        System.out.println("Hello");
                                    }
                                    int b = 5;
                                    int c = 5;
                                    int d = 5;
                                    int e = 5;
                                    int f = 5;
                                    System.out.println(b + c + d + e + f);
                                    
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }
                            }
                        }
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        assertTrue(problems.hasNext(), "At least one problem should be reported");
        assertEqualsTryCatchComplexity(problems.next());
        problems.assertExhausted();
    }

    // deleted empty if statement to be in the range of allowed statements
    @Test
    void testNoFalsePositive() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                    int a = 5;
                                    if (a == 5) {
                                        System.out.println("Hello");
                                        for (int i = 0; i < 10; i++) {
                                            int c = 5;
                                            int f = c + 6;
                                            System.out.println(f);
                                        }
                                    }
                                    for (int i = 0; i < 2; i++) {
                                        System.out.println("Hello");
                                    }
                                    int b = 5;
                                    int c = 5;
                                    int d = 5;
                                    int e = 5;
                                    int f = 5;
                                    System.out.println(b + c + d + e + f);
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }

                            }
                        }
                        
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        problems.assertExhausted();
    }
    @Test
    void testMethodInvocations() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                   test();
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }
                            }
                            public static void test() {
                                int a = 1;
                                int b = 2;
                                int c = 3;
                                int d = 4;
                                int e = 5;
                                int f = 6;
                                int g = 7;
                                int h = 8;
                                int i = 9;
                                int j = 10;
                                int k = 11;
                                int l = 12;
                                int m = 13;
                                int n = 14;
                                int o = 15;
                                int p = 16;
                                System.out.println(a + b + c + d + e + f +
                                    g + h + i + j + k + l + m + n + o + p);
                            }
                        }
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        problems.assertExhausted();
    }
    @Test
    void testSwitch() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                   String test = "Hello";
                                   switch(test) {
                                        case "Hello":
                                            System.out.println("Hello");
                                            break;
                                        case "World":
                                            System.out.println("World");
                                            break;
                                        case "Test":
                                            System.out.println("Test");
                                            System.out.println("Test");
                                            break;
                                        default:
                                            System.out.println("Default");
                                            System.out.println("Finished");
                                            break;
                                   }
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }

                            }
                        }
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        assertTrue(problems.hasNext(), "At least one problem should be reported");
        assertEqualsTryCatchComplexity(problems.next());
    }
    @Test
    void testSomeLoops() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                    String[] ls = new String[]{"Hello", "World", "Test"};
                                    for (String s : ls) {
                                        int i = 0;
                                        while (i < ls.length) {
                                            System.out.println(s);
                                            i++;
                                            int j = 0;
                                            do {
                                                System.out.println(s);
                                                j++;
                                            } while (j < ls.length);
                                        }
                                        System.out.println(s);
                                        System.out.println("%s %s".formatted(s, s));
                                    }
                                    int a = 5;
                                    int b = 5;
                                    int c = 5;
                                    System.out.println(a + b + c);
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }

                            }
                        }
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        assertTrue(problems.hasNext(), "At least one problem should be reported");
        assertEqualsTryCatchComplexity(problems.next());
    }
    @Test
    void testNestedTry() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
                "Main",
                """
                        public class Main {
                            public static void main(String ...args) {
                                try {
                                    try {
                                        int a = 1;
                                        int b = 2;
                                        int c = 3;
                                        int d = 4;
                                        int e = 5;
                                        hallo();
                                    } catch (Exception e) {
                                        int g = 7;
                                        int h = 8;
                                        int i = 9;
                                        int j = 10;
                                        int k = 11;
                                        int l = 12;
                                        int m = 13;
                                        int n = 14;
                                        hallo();
                                    }
                                } catch (Exception e) {
                                    System.out.println("Hello");
                                }
                            }
                            public static void hallo() {
                                String a = "ooops";
                            }
                        }
                        """
        ), List.of(ProblemType.TRY_CATCH_COMPLEXITY));
        assertTrue(problems.hasNext(), "At least one problem should be reported");
        assertEqualsTryCatchComplexity(problems.next());
    }
}


