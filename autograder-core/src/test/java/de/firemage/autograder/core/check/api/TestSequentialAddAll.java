package de.firemage.autograder.core.check.api;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSequentialAddAll extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.SEQUENTIAL_ADD_ALL,
        ProblemType.USE_ENUM_VALUES
    );

    private void assertEqualsReimplementation(Problem problem, String type, Iterable<String> values, String collection) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of(
                        "suggestion", "private static final List<%s> SOME_GOOD_NAME = List.of(%s); /* ... */ %s.addAll(SOME_GOOD_NAME)".formatted(
                            type,
                            String.join(", ", values),
                            collection
                        )
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    private void assertEqualsEnumValues(Problem problem, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of(
                        "suggestion", suggestion
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testListAdd() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                public class Test {
                    public void foo(List<String> list) {
                        list.add(" ");
                        list.add("a");
                        list.add("b");
                        list.add("c");
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "String", List.of("\" \"", "\"a\"", "\"b\"", "\"c\""), "list");

        problems.assertExhausted();
    }

    @Test
    void testListAddPartiallyConstant() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                public class Test {
                    public void foo(List<String> list, String value) {
                        list.add(" ");
                        list.add("a");
                        list.add("b");
                        list.add(value);
                        list.add("c");
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testListAddConstants() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                public class Test {
                    private static final String A = "a";
                    private static final String B = "b";
                    private static final String C = "c";
                    private static final String D = "d";
                    
                    public void foo(List<String> list) {
                        list.add(A);
                        list.add(B);
                        list.add(C);
                        list.add(D);
                        list.add(D);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "String", List.of("A", "B", "C", "D", "D"), "list");

        problems.assertExhausted();
    }

    @Test
    void testEnumValuesAddAllUnorderedSet() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.HashSet;
                import java.util.Set;

                enum Fruit {
                    APPLE, BANANA, CHERRY;
                }

                public class Test {
                    public static void main(String[] args) {
                        Set<Fruit> fruits = new HashSet<>();
                        
                        fruits.add(Fruit.APPLE);
                        fruits.add(Fruit.BANANA);
                        fruits.add(Fruit.CHERRY);

                        System.out.println(fruits);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsEnumValues(problems.next(), "fruits.addAll(Arrays.asList(Fruit.values()))");
        problems.assertExhausted();
    }

    @Test
    void testEnumValuesAddAllOrderedList() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;

                enum GodCard {
                    APOLLO,
                    ARTEMIS,
                    ATHENA,
                    ATLAS,
                    DEMETER,
                    HERMES;
                }

                public class Test {
                    private static List<GodCard> getAvailableCards() {
                        List<GodCard> availableCards = new ArrayList<>();

                        availableCards.add(GodCard.APOLLO);
                        availableCards.add(GodCard.ARTEMIS);
                        availableCards.add(GodCard.ATHENA);
                        availableCards.add(GodCard.ATLAS);
                        availableCards.add(GodCard.DEMETER);
                        availableCards.add(GodCard.HERMES);

                        return availableCards;
                    }

                    // NOTE: Enum.values() returns the variants in the order they are declared
                    //       For Sets this is not a problem, but for Lists the order in which add
                    //       is called is important
                    private static List<GodCard> getReversedAvailableGodCards() {
                        List<GodCard> availableCards = new ArrayList<>();

                        availableCards.add(GodCard.HERMES);
                        availableCards.add(GodCard.DEMETER);
                        availableCards.add(GodCard.ATLAS);
                        availableCards.add(GodCard.APOLLO);
                        availableCards.add(GodCard.ATHENA);
                        availableCards.add(GodCard.ARTEMIS);

                        return availableCards;
                    }
                    
                    private static List<GodCard> getAvailableCardsDuplicateMiddle() {
                        List<GodCard> availableCards = new ArrayList<>();

                        availableCards.add(GodCard.APOLLO);
                        availableCards.add(GodCard.ARTEMIS);
                        availableCards.add(GodCard.ATHENA);
                        availableCards.add(GodCard.ATLAS);
                        availableCards.add(GodCard.ATLAS);
                        availableCards.add(GodCard.DEMETER);
                        availableCards.add(GodCard.HERMES);

                        return availableCards;
                    }

                    private static List<GodCard> getAvailableCardsDuplicateEnd() {
                        List<GodCard> availableCards = new ArrayList<>();

                        availableCards.add(GodCard.APOLLO);
                        availableCards.add(GodCard.ARTEMIS);
                        availableCards.add(GodCard.ATHENA);
                        availableCards.add(GodCard.ATLAS);
                        availableCards.add(GodCard.DEMETER);
                        availableCards.add(GodCard.HERMES);
                        availableCards.add(GodCard.HERMES);

                        return availableCards;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsEnumValues(problems.next(), "availableCards.addAll(Arrays.asList(GodCard.values()))");
        assertEqualsReimplementation(problems.next(), "GodCard", List.of("GodCard.HERMES", "GodCard.DEMETER", "GodCard.ATLAS", "GodCard.APOLLO", "GodCard.ATHENA", "GodCard.ARTEMIS"), "availableCards");
        assertEqualsReimplementation(problems.next(), "GodCard", List.of("GodCard.APOLLO", "GodCard.ARTEMIS", "GodCard.ATHENA", "GodCard.ATLAS", "GodCard.ATLAS", "GodCard.DEMETER", "GodCard.HERMES"), "availableCards");
        assertEqualsReimplementation(problems.next(), "GodCard", List.of("GodCard.APOLLO", "GodCard.ARTEMIS", "GodCard.ATHENA", "GodCard.ATLAS", "GodCard.DEMETER", "GodCard.HERMES", "GodCard.HERMES"), "availableCards");
        problems.assertExhausted();
    }
}


