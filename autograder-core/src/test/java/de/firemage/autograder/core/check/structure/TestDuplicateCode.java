package de.firemage.autograder.core.check.structure;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDuplicateCode extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.DUPLICATE_CODE);

    void assertDuplicateCode(Problem problem, String left, String right) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "duplicate-code",
                Map.of(
                    "left", left,
                    "right", right
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testDuplicateDifferingNonConstantExpression() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "StringUtils",
            """
                public class StringUtils {
                    public static String ceasarEncryption(String input, int key) {
                        char[] inputArray = input.toCharArray();
                        for (int i = 0; i < inputArray.length; i++) {
                            if (Character.isUpperCase(inputArray[i])) {
                                inputArray[i] = (char) (((inputArray[i] - 65 + key) % 26) + 65);
                            } else {
                                inputArray[i] = (char) (((inputArray[i] - 97 + key) % 26) + 97);
                            }
                        }
                        return new String(inputArray);
                    }

                    public static String ceasarDecryption(String input, int key) {
                        char[] inputArray = input.toCharArray();
                        for (int i = 0; i < inputArray.length; i++) {
                            if (Character.isUpperCase(inputArray[i])) {
                                inputArray[i] = (char) (((inputArray[i] - 65 + (26 - key)) % 26) + 65);
                            } else {
                                inputArray[i] = (char) (((inputArray[i] - 97 + (26 - key)) % 26) + 97);
                            }
                        }
                        return new String(inputArray);
                    }
                }
                // the two methods are mostly identical except for two assigned values
                // the decryption has a `26 - key` while the encryption has `key`
                // key is a param, so the new utility method could have an extra param that applies that shift
                """
        ), PROBLEM_TYPES);

        assertDuplicateCode(problems.next(), "StringUtils:3-11", "StringUtils:15-23");

        problems.assertExhausted();
    }


    @Test
    void testTooManyVariablesBeforeDuplicate() throws IOException, LinterException {
        // In this test, there is a large duplicate segment of code that writes to variables
        // declared in the duplicate segment.
        //
        // The declared variables are used after the duplicate segment, therefore if one extracts
        // the duplicate segment into a method, the variables would have to be returned from the
        // method or passed as a parameter (this only works for mutable types like collections or arrays).
        // TODO: write test for the collection/array case?
        // TODO: we can only pass them as parameter if they are never assigned a new value (excluding arrays)
        //
        // A method can only return one value. One could work around this by creating a tuple like class (Pair, Triple, ...),
        // but this is not a good solution, therefore the duplicate code should be ignored in this case.

        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";
                        char[] array = new char[input.length()];
                        String other = "other";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                                other += result;
                            }
                            System.out.println(result);
                            array[i] = c;
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                        System.out.println(array);
                    }

                    public static void b(String input) {
                        String result = "";
                        char[] array = new char[input.length()];
                        String other = "other";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                                other += result;
                            }
                            System.out.println(result);
                            array[i] = c;
                        }

                        // force an end to the duplicate segment
                        callB();
                        
                        System.out.println(result);
                        System.out.println(array);
                        System.out.println(other);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testReturnsSingleVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                    }

                    public static void b(String input) {
                        String result = "";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                        }

                        // force an end to the duplicate segment
                        callB();

                        System.out.println(result);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicateCode(problems.next(), "Main:11-21", "Main:30-40");

        problems.assertExhausted();
    }

    @Test
    void testReassignsSingleVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";

                        callA();

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                    }

                    public static void b(String input) {
                        String result = "";

                        callB();

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                        }

                        // force an end to the duplicate segment
                        callB();

                        System.out.println(result);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicateCode(problems.next(), "Main:15-23", "Main:36-44");

        problems.assertExhausted();
    }

    @Test
    void testReassignsSingleVariableWithUnrelated() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";
                        String other = "";

                        callA();

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                        System.out.println(other);
                    }

                    public static void b(String input) {
                        String result = "";
                        String other = "";

                        callB();

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                        }

                        // force an end to the duplicate segment
                        callB();

                        System.out.println(result);
                        System.out.println(other);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicateCode(problems.next(), "Main:16-24", "Main:39-47");

        problems.assertExhausted();
    }

    @Test
    void testReassignsMultipleVariablesUsedAfterDuplicate() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";
                        String other = "other";

                        callA();

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                            other += result;
                            System.out.println(other);
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                        System.out.println(other);
                    }

                    public static void b(String input) {
                        String result = "";
                        String left = "other";

                        callB();

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                            left += result;
                            System.out.println(left);
                        }

                        // force an end to the duplicate segment
                        callB();
                        
                        System.out.println(result);
                        System.out.println(left);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testReassignsMultipleVariablesOnlyUsedInDuplicate() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";

                        callA();
                        String other = "other";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                            other += result;
                            System.out.println(other);
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                    }

                    public static void b(String input) {
                        String result = "";

                        callB();
                        String other = "other";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                            other += result;
                            System.out.println(other);
                        }

                        // force an end to the duplicate segment
                        callB();
                        
                        System.out.println(result);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicateCode(problems.next(), "Main:14-26", "Main:38-50");

        problems.assertExhausted();
    }

    @Test
    @Disabled("Will be implemented in the future")
    void testReassignsMultipleVariablesOnlyUsedInDuplicateRenamed() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    public static void callA() {
                        System.out.println("A");
                    }

                    public static void callB() {
                        System.out.println("B");
                    }

                    public static void a(String input) {
                        String result = "";

                        callA();
                        String other = "other";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                            other += result;
                            System.out.println(other);
                        }

                        // force an end to the duplicate segment
                        callA();
                        
                        System.out.println(result);
                    }

                    public static void b(String input) {
                        String result = "";

                        callB();
                        String left = "other";

                        for (int i = 0; i < input.length(); i++) {
                            char c = input.charAt(i);
                            if (Character.isLetter(c)) {
                                result += c;
                            }
                            System.out.println(result);
                            System.out.println(c);
                            System.out.println(c);
                            left += result;
                            System.out.println(left);
                        }

                        // force an end to the duplicate segment
                        callB();
                        
                        System.out.println(result);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertDuplicateCode(problems.next(), "Main:14-26", "Main:38-50");

        problems.assertExhausted();
    }
}


