package de.firemage.autograder.core.check.exceptions;
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

class TestTryBlockSize extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.TRY_BLOCK_SIZE);

    void assertUnnecessaryStatements(Problem problem, String lines) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "try-block-size",
                Map.of(
                    "lines", lines
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testNothingThrows() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                public class Main {
                    private Main() {
                    }

                    public static void main(String[] args) {
                        try {
                            System.out.println("Hello");
                            System.out.println("World");
                            System.out.println("!");
                        } catch (Exception e) {
                            System.out.println("Error");
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUnnecessaryStatements(problems.next(), "L7-9");

        problems.assertExhausted();
    }

    @Test
    void testMotivation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.Scanner;
                
                class InvalidArgumentException extends Exception {
                    public InvalidArgumentException(String message) {
                        super(message);
                    }
                }

                public class Main {
                    private Main() {
                    }

                    public static void main(String[] args) {
                        try {
                            Scanner scanner = new Scanner(System.in);

                            int[] array = new int[5];
                            int index = 0;
                            String line = scanner.nextLine();
                            
                            for (String part : line.split(":", -1)) {
                                array[index] = parseNumber(part);
                                index += 1;
                            }

                            for (int i = 0; i < array.length; i++) {
                                System.out.println(array[i]);
                            }
                        } catch (InvalidArgumentException e) {
                            System.out.println("Error, %s".formatted(e.getMessage()));
                        }
                    }
                    
                    private static int parseNumber(String string) throws InvalidArgumentException {
                        try {
                            return Integer.parseInt(string);
                        } catch (NumberFormatException e) {
                            throw new InvalidArgumentException("The input \\"%s\\" is not a valid number.".formatted(string));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUnnecessaryStatements(problems.next(), "L15-19, L26-28");

        problems.assertExhausted();
    }

    @Test
    void testOnlyTrailing() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.Scanner;
                
                class InvalidArgumentException extends Exception {
                    public InvalidArgumentException(String message) {
                        super(message);
                    }
                }

                public class Main {
                    private Main() {
                    }

                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                            
                        int[] array = new int[5];
                        int index = 0;
                        String line = scanner.nextLine();

                        try {
                            for (String part : line.split(":", -1)) {
                                array[index] = parseNumber(part);
                                index += 1;
                            }

                            for (int i = 0; i < array.length; i++) {
                                System.out.println(array[i]);
                            }
                        } catch (InvalidArgumentException e) {
                            System.out.println("Error, %s".formatted(e.getMessage()));
                        }
                    }
                    
                    private static int parseNumber(String string) throws InvalidArgumentException {
                        try {
                            return Integer.parseInt(string);
                        } catch (NumberFormatException e) {
                            throw new InvalidArgumentException("The input \\"%s\\" is not a valid number.".formatted(string));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUnnecessaryStatements(problems.next(), "L26-28");

        problems.assertExhausted();
    }


    @Test
    void testUsesVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.Scanner;
                
                class InvalidArgumentException extends Exception {
                    public InvalidArgumentException(String message) {
                        super(message);
                    }
                }

                public class Main {
                    private Main() {
                    }

                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        String line = scanner.nextLine();

                        try {
                            int value = parseNumber(line);

                            for (int i = 0; i < 5; i++) {
                                System.out.println(value);
                            }
                            System.out.println(value);
                        } catch (InvalidArgumentException e) {
                            System.out.println("Error, %s".formatted(e.getMessage()));
                        }
                    }

                    private static int parseNumber(String string) throws InvalidArgumentException {
                        try {
                            return Integer.parseInt(string);
                        } catch (NumberFormatException e) {
                            throw new InvalidArgumentException("The input \\"%s\\" is not a valid number.".formatted(string));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUnnecessaryStatements(problems.next(), "L20-23");

        problems.assertExhausted();
    }
}


