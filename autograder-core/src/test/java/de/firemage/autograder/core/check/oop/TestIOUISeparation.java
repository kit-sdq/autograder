package de.firemage.autograder.core.check.oop;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestIOUISeparation extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.UI_INPUT_SEPARATION, ProblemType.UI_OUTPUT_SEPARATION);

    void assertEqualsInput(Problem problem, String location) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "ui-input-separation",
                Map.of(
                    "first", location
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsOutput(Problem problem, String location) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "ui-output-separation",
                Map.of(
                    "first", location
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    Map.Entry<String, String> makeClass(String packageName, String className, Collection<String> uses) {
        String name = packageName.isEmpty() ? className : packageName + "." + className;

        return Map.entry(
            name,
            """
            %s
            
            %s

            public class %s {
                %s
                
                %s
            }
            """.formatted(
                packageName.isEmpty() ? "" : "package %s;\n\n".formatted(packageName),
                uses.stream().anyMatch(use -> use.contains("Scanner")) ? "import java.util.Scanner;" : "",
                className,
                String.join("\n\n", uses),
                className.equals("Main") ? "public static void main(String[] args) {}" : ""
            )
        );
    }

    Map.Entry<String, String> makeClass(String packageName, String className, boolean isUsingScanner, boolean isUsingPrint) {
        Collection<String> uses = new ArrayList<>();
        if (isUsingScanner) {
            uses.add("""
                public static String useScanner() {
                    Scanner scanner = new Scanner(System.in);
                    return scanner.nextLine();
                }
                """);
        }

        if (isUsingPrint) {
            uses.add("""
                public static void usePrint() {
                    System.out.println("Hello World!");
                }
                """);
        }


        return this.makeClass(packageName, className, uses);
    }

    @Test
    void testSingleClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Scanner;

                public class Test {
                    public Test() {
                        Scanner scanner = new Scanner(System.in); /*# ok #*/

                        String input = scanner.nextLine(); /*# ok #*/
                        System.out.println("Test!"); /*# ok #*/
                    }

                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in); /*# ok #*/
                        String input = scanner.nextLine(); /*# ok #*/

                        System.out.println("Hello World!"); /*# ok #*/
                        System.out.print("Hello World!"); /*# ok #*/
                    }

                    public static void eprint() {
                        System.err.println(); /*# ok #*/
                        Scanner scanner = new Scanner(System.in); /*# ok #*/
                        int input = scanner.nextInt(); /*# ok #*/
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSinglePackageRoot() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("", "Test", true, true),
                makeClass("", "Other", true, true)
            )
        ), PROBLEM_TYPES);

        assertEqualsInput(problems.next(), "Other:L8");
        assertEqualsOutput(problems.next(), "Other:L13");

        problems.assertExhausted();
    }

    @Test
    void testSinglePackageNotRoot() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "a.Test",
                    """
                        package a;

                        import java.util.Scanner;

                        public class Test {
                            public Test() {
                                Scanner scanner = new Scanner(System.in); /*# ok #*/
                                
                                String input = scanner.nextLine(); /*# ok #*/
                                System.out.println("Test!"); /*# ok #*/
                            }
                        }
                        """
                ),
                Map.entry(
                    "a.Other",
                    """
                        package a;

                        import java.util.Scanner;

                        public class Other {
                            public static void eprint() {
                                System.err.println(); /*# ok #*/
                                Scanner scanner = new Scanner(System.in); /*# ok #*/
                                int input = scanner.nextInt(); /*# ok #*/
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsInput(problems.next(), "Other:L9");
        assertEqualsOutput(problems.next(), "Other:L7");

        problems.assertExhausted();
    }

    @Test
    void testMultiplePackagesRootOk() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("", "Test", true, true),
                makeClass("", "Other", true, true),
                makeClass("a", "First", false, false),
                makeClass("a", "Second", false, false)
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultiplePackagesOkNotInRoot() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("", "Test", false, false),
                makeClass("", "Other", false, false),
                makeClass("a", "First", true, true),
                makeClass("a", "Second", true, true)
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultiplePackagesOk() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("b", "Test", true, true),
                makeClass("b", "Other", true, true),
                makeClass("a", "First", false, false),
                makeClass("a", "Second", false, false)
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultiplePackagesUsedInOneClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("b", "Inputs", true, false),
                makeClass("b", "Outputs", false, true),
                makeClass("a", "First", false, false)
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultiplePackagesOutputOkInputNotOk() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("b", "Inputs", true, false),
                makeClass("b", "Outputs", false, true),
                makeClass("a", "First", true, false)
            )
        ), PROBLEM_TYPES);

        assertEqualsInput(problems.next(), "First:L10");

        problems.assertExhausted();
    }

    @Test
    void testMultiplePackagesAndMain() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("b", "Inputs", true, false),
                makeClass("b", "Outputs", false, true),
                makeClass("a", "First", false, false),
                makeClass("a", "Main", true, true)
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSinglePackageAndMain() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                makeClass("b", "Inputs", true, false),
                makeClass("b", "Outputs", false, true),
                makeClass("b", "First", false, false),
                makeClass("b", "Main", true, true)
                )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


