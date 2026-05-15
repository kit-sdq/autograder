package de.firemage.autograder.core.check.oop;

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

class TestClosedSetOfValues extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.CLOSED_SET_OF_VALUES);

    void assertClosedSetSwitch(Problem problem, String values) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "closed-set-of-values-switch",
                Map.of(
                    "values", values
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertClosedSetListing(Problem problem, String values) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "closed-set-of-values-list",
                Map.of(
                    "values", values
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertClosedSetMethod(Problem problem, String values) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "closed-set-of-values-method",
                Map.of(
                    "values", values
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSwitchInEnum() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Color",
            """
                public enum Color {
                    RED,
                    GREEN,
                    BLUE,
                    YELLOW;

                    public static Color fromString(String value) {
                        switch (value) { /*# ok #*/
                            case "red":
                                return RED;
                            case "green":
                                return GREEN;
                            case "blue":
                                return BLUE;
                            case "yellow":
                                return YELLOW;
                            default:
                                throw new IllegalArgumentException("Unknown color: " + value);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSwitchWithCode() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        switch (args[0]) {
                            case "red" -> {
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                            }
                            case "green" -> {
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                            }
                            case "blue" -> {
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                            }
                            case "yellow" -> {
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                                System.out.println("Executing some code...");
                            }
                            default ->
                                throw new IllegalArgumentException("Unknown color: " + args[0]);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertClosedSetSwitch(problems.next(), "\"red\", \"green\", \"blue\", \"yellow\"");

        problems.assertExhausted();
    }

    @Test
    void testMapToEnum() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                enum Color {
                    RED,
                    GREEN,
                    BLUE,
                    YELLOW;
                }
                
                public class Test {
                    private static Color fromString(String value) {
                        switch (value) { /*# ok #*/
                            case "red":
                                return Color.RED;
                            case "green":
                                return Color.GREEN;
                            case "blue":
                                return Color.BLUE;
                            case "yellow":
                                return Color.YELLOW;
                            default:
                                throw new IllegalArgumentException("Unknown color: " + value);
                        }
                    }
                    
                    public static void main(String[] args) {
                        System.out.println(fromString("red"));
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMapToClasses() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Color",
                    """
                    public interface Color {}
                    """
                ),
                Map.entry(
                    "Red",
                    """
                    public class Red implements Color {}
                    """
                ),
                Map.entry(
                    "Green",
                    """
                    public class Green implements Color {}
                    """
                ),
                Map.entry(
                    "Blue",
                    """
                    public class Blue implements Color {}
                    """
                ),
                Map.entry(
                    "Yellow",
                    """
                    public class Yellow implements Color {}
                    """
                ),
                Map.entry(
                    "Test",
                    """
                        public class Test {
                            private static Color fromString(String value) {
                                switch (value) {
                                    case "red":
                                        return new Red();
                                    case "green":
                                        return new Green();
                                    case "blue":
                                        return new Blue();
                                    case "yellow":
                                        return new Yellow();
                                    default:
                                        throw new IllegalArgumentException("Unknown color: " + value);
                                }
                            }
                            
                            public static void main(String[] args) {
                                System.out.println(fromString("red"));
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMapToClassesSwitchExpr() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Color",
                    """
                    public interface Color {}
                    """
                ),
                Map.entry(
                    "Red",
                    """
                    public class Red implements Color {}
                    """
                ),
                Map.entry(
                    "Green",
                    """
                    public class Green implements Color {}
                    """
                ),
                Map.entry(
                    "Blue",
                    """
                    public class Blue implements Color {}
                    """
                ),
                Map.entry(
                    "Yellow",
                    """
                    public class Yellow implements Color {}
                    """
                ),
                Map.entry(
                    "Test",
                    """
                        public class Test {
                            private static Color fromString(String value) {
                                return switch (value) {
                                    case "red" -> new Red();
                                    case "green" -> new Green();
                                    case "blue" -> new Blue();
                                    case "yellow" -> new Yellow();
                                    default -> throw new IllegalArgumentException("Unknown color: " + value);
                                };
                            }
                            
                            public static void main(String[] args) {
                                System.out.println(fromString("red"));
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMapToClassesInvocation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Color",
                    """
                    public interface Color {}
                    """
                ),
                Map.entry(
                    "Red",
                    """
                    public class Red implements Color {
                        public static Color of() {
                            return new Red();
                        }
                    }
                    """
                ),
                Map.entry(
                    "Green",
                    """
                    public class Green implements Color {
                        public static Color of() {
                            return new Green();
                        }
                    }
                    """
                ),
                Map.entry(
                    "Blue",
                    """
                    public class Blue implements Color {
                        public static Color of() {
                            return new Blue();
                        }
                    }
                    """
                ),
                Map.entry(
                    "Yellow",
                    """
                    public class Yellow implements Color {
                        public static Color of() {
                            return new Yellow();
                        }
                    }
                    """
                ),
                Map.entry(
                    "Test",
                    """
                        public class Test {
                            private static Color fromString(String value) {
                                return switch (value) {
                                    case "red" -> Red.of();
                                    case "green" -> Green.of();
                                    case "blue" -> Blue.of();
                                    case "yellow" -> Yellow.of();
                                    default -> throw new IllegalArgumentException("Unknown color: " + value);
                                };
                            }
                            
                            public static void main(String[] args) {
                                System.out.println(fromString("red"));
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMapToClassesAssignment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Color",
                    """
                    public interface Color {}
                    """
                ),
                Map.entry(
                    "Red",
                    """
                    public class Red implements Color {}
                    """
                ),
                Map.entry(
                    "Green",
                    """
                    public class Green implements Color {}
                    """
                ),
                Map.entry(
                    "Blue",
                    """
                    public class Blue implements Color {}
                    """
                ),
                Map.entry(
                    "Yellow",
                    """
                    public class Yellow implements Color {}
                    """
                ),
                Map.entry(
                    "Test",
                    """
                        public class Test {
                            private static Color fromString(String value) {
                                Color result = null;
                                switch (value) {
                                    case "red":
                                        result = new Red();
                                        break;
                                    case "green":
                                        result = new Green();
                                        break;
                                    case "blue":
                                        result = new Blue();
                                        break;
                                    case "yellow":
                                        result = new Yellow();
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown color: " + value);
                                }

                                return result;
                            }
                            
                            public static void main(String[] args) {
                                System.out.println(fromString("red"));
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testReturnClosedSet() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    public String getDayOfWeek(int day) { /*# not ok #*/
                        if (day == 1) {
                            return "monday";
                        }

                        if (day == 2) {
                            return "tuesday";
                        }

                        if (day == 3) {
                            return "wednesday";
                        }

                        if (day == 4) {
                            return "thursday";
                        }

                        if (day == 5) {
                            return "friday";
                        }

                        if (day == 6) {
                            return "saturday";
                        }

                        if (day == 7) {
                            return "sunday";
                        }

                        throw new IllegalArgumentException("Invalid day: " + day);
                    }

                    public String getDayOfWeek2(int day) {
                        if (day == 1) {
                            return "monday";
                        }

                        if (day == 2) {
                            return "tuesday";
                        }

                        if (day == 3) {
                            return "wednesday";
                        }

                        if (day == 4) {
                            return "thursday";
                        }

                        if (day == 5) {
                            return "friday";
                        }

                        if (day == 6) {
                            return "saturday";
                        }

                        if (day == 7) {
                            return "sunday";
                        }

                        return null;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertClosedSetMethod(problems.next(), "\"monday\", \"tuesday\", \"wednesday\", \"thursday\", \"friday\", \"saturday\", \"sunday\"");

        problems.assertExhausted();
    }

    @Test
    void testFiniteListingList() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                public class Test {
                    private static final List<Character> VALID_LIST = List.of('R', 'x', 'X', '|', '\\\\', '/', '_', '*', ' ');
                    private static final List<Character> NOT_ENOUGH_DISTINCT_VALUES_LIST = List.of('a', 'b', 'b', 'a'); /*# ok; 2 distinct values #*/
                }
                """
        ), PROBLEM_TYPES);

        assertClosedSetListing(problems.next(), "'R', 'x', 'X', '|', '\\\\', '/', '_', '*', ' '");

        problems.assertExhausted();
    }

    @Test
    void testFiniteListingArray() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final char[] VALID_ARRAY = {'R', 'x', 'X', '|', '\\\\', '/', '_', '*', ' '}; /*# not ok #*/
                    private static final char[] NOT_ENOUGH_DISTINCT_VALUES_ARRAY = { 'a', 'b', 'b', 'a' }; /*# ok; 2 distinct values #*/
                }
                """
        ), PROBLEM_TYPES);

        assertClosedSetListing(problems.next(), "'R', 'x', 'X', '|', '\\\\', '/', '_', '*', ' '");

        problems.assertExhausted();
    }

    @Test
    void testFiniteListingSet() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;

                public class Test {
                    private static final Set<String> FINITE_SET = Set.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"); /*# not ok #*/
                    private static final Set<String> NOT_ENOUGH_DISTINCT_VALUES_SET = Set.of("monday", "monday", "monday", "monday"); /*# ok; 1 distinct value #*/
                }
                """
        ), PROBLEM_TYPES);

        assertClosedSetListing(problems.next(), "\"monday\", \"tuesday\", \"wednesday\", \"thursday\", \"friday\", \"saturday\", \"sunday\"");

        problems.assertExhausted();
    }

    @Test
    void testFiniteListingLocalVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Set;

                public class Test {
                    public static void main(String[] args) {
                        String[] godFavors = {"HW", "IR", "MW", "TS", "TT", "VB", "GHW", "GIR", "GMW", "GTS", "GTT", "GVB"}; //# Not ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertClosedSetListing(problems.next(), "\"HW\", \"IR\", \"MW\", \"TS\", \"TT\", \"VB\", \"GHW\", \"GIR\", \"GMW\", \"GTS\", \"GTT\", \"GVB\"");

        problems.assertExhausted();
    }
}


