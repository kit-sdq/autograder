package de.firemage.autograder.extra.check.naming;

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

class TestVariablesHaveDescriptiveNamesCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.SINGLE_LETTER_LOCAL_NAME,
        ProblemType.IDENTIFIER_IS_ABBREVIATED_TYPE,
        ProblemType.IDENTIFIER_CONTAINS_TYPE_NAME,
        ProblemType.SIMILAR_IDENTIFIER,
        ProblemType.IDENTIFIER_REDUNDANT_NUMBER_SUFFIX
    );

    private void assertInternal(Problem problem, String key, String name) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    key,
                    Map.of(
                        "name", name
                    )
                )
            ),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    // "variable-name-single-letter"
    // "variable-is-abbreviation"
    // "variable-name-type-in-name"
    // "variable-redundant-number-suffix"
    // "similar-identifier"

    private void assertSingleLetter(Problem problem, String name) {
        assertInternal(problem, "variable-name-single-letter", name);
    }

    private void assertAbbreviation(Problem problem, String name) {
        assertInternal(problem, "variable-is-abbreviation", name);
    }

    private void assertTypeInName(Problem problem, String name) {
        assertInternal(problem, "variable-name-type-in-name", name);
    }

    private void assertRedundantNumberSuffix(Problem problem, String name) {
        assertInternal(problem, "variable-redundant-number-suffix", name);
    }

    private void assertSimilarIdentifier(Problem problem, String left, String right) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "similar-identifier",
                    Map.of(
                        "left", left,
                        "right", right
                    )
                )
            ),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testNonUnicodeName() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                enum Monat {
                    Januar,
                    Februar,
                    März,
                    April,
                    Mai,
                    August,
                    September,
                    Oktober,
                    November,
                    Dezember
                }
            
                class Kalenderdatum {
                    private double jahr;
                    private Monat monat;
                    private byte tag;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testSimple() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.*;

            public class Test {
                private static List<String> list; /*# ok #*/
                private static List<String> exampleList; /*# not ok #*/
                private static int[] exampleIntArray; /*# not ok #*/
                private static int[] intArray; /*# not ok #*/
                private static String s; /*# not ok #*/
                private static String string; /*# ok #*/

                int trafficLight1;
                int trafficLight2; /*# not ok; similar to trafficLight1 #*/
                int trafficLight3; /*# not ok; similar to trafficLight1 #*/

                int result1; /*# not ok; could be result #*/

                int sec; /*# not ok #*/
                int min; /*# not ok #*/
                int interpret; /*# ok #*/
                String interpreters; /*# ok #*/

                List<String> validStrings; /*# not ok #*/
                Set<Integer> integerSet; /*# not ok #*/
                int pointer; /*# ok #*/
                int pointerValue; /*# ok #*/
                int flagInternal; /*# ok #*/
                int playerPointer; /*# ok #*/
                int maxValue; /*# ok #*/
                int minValue; /*# ok #*/
                int maximumValue; /*# ok #*/
                int minimumValue; /*# ok #*/
                int maxNumber; /*# ok #*/
                int minNumber; /*# ok #*/
                int max_number; /*# ok; wrong case, previoulsy resulted in a crash #*/

                private void test() {
                    Test[] tests = new Test[5];
                    for (Test test : tests) { } /*# ok #*/
                    for (char c: "abc".toCharArray()) { } /*# ok #*/
                }

                String datePattern; /*# ok #*/
                String namePattern; /*# ok #*/
                String timePattern; /*# ok #*/

                String subString; /*# ok #*/
                List<String> subList; /*# ok #*/

                int x1; /*# ok #*/
                int x2; /*# ok #*/
                int x3; /*# ok #*/

                int y1; /*# ok #*/
                int y2; /*# ok #*/
                int y3; /*# ok #*/
            }

            enum Month {
                JANUARY,
                FEBRUARY,
                MARCH,
                APRIL,
                MAY,
                JUNE, /*# ok #*/
                JULY, /*# ok #*/
                AUGUST,
                SEPTEMBER,
                OCTOBER,
                NOVEMBER,
                DECEMBER
            }

            // crashes if implicit identifiers are not ignored
            enum BuildingType {
                HOUSE("H") {
                @Override
                public boolean isHousing() {
                    return true;
                }
            };

            private final String symbol;

            BuildingType(String symbol) {
                this.symbol = symbol;
            }

            public abstract boolean isHousing();
            }
            """
        ), PROBLEM_TYPES);

        assertTypeInName(problems.next(), "exampleList");
        assertTypeInName(problems.next(), "exampleIntArray");
        assertTypeInName(problems.next(), "intArray");
        assertSingleLetter(problems.next(), "s");
        assertSimilarIdentifier(problems.next(), "trafficLight1", "trafficLight2");
        assertSimilarIdentifier(problems.next(), "trafficLight1", "trafficLight3");
        assertRedundantNumberSuffix(problems.next(), "result1");
        assertAbbreviation(problems.next(), "sec");
        assertAbbreviation(problems.next(), "min");
        assertTypeInName(problems.next(), "validStrings");
        assertTypeInName(problems.next(), "integerSet");

        problems.assertExhausted();
    }
}
