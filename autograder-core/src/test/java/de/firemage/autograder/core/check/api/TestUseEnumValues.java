package de.firemage.autograder.core.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseEnumValues extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_ENUM_VALUES);

    private void assertEqualsReimplementation(Problem problem, String suggestion) {
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
    void testEnumValuesListing() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.HashSet;
                import java.util.Set;
                import java.util.ArrayList;
                import java.util.List;

                enum Fruit {
                    APPLE, BANANA, CHERRY;
                }

                public class Test {
                    private static final List<Fruit> ORDERED_LIST_FRUITS = List.of(Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY);
                    private static final List<Fruit> UNORDERED_LIST_FRUITS = List.of(Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);
                    private static final Set<Fruit> UNORDERED_SET_FRUITS = Set.of(Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);
                    private static final Set<Fruit> ORDERED_SET_FRUITS = Set.of(Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY);
                    
                    private static final Fruit[] ORDERED_ARRAY_FRUITS = new Fruit[] {Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY};
                    private static final Fruit[] UNORDERED_ARRAY_FRUITS = {Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY};
                    
                    private static final Fruit[] DUPLICATE_ARRAY = {Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY, Fruit.CHERRY};
                    private static final List<Fruit> DUPLICATE_LIST = List.of(Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY, Fruit.CHERRY);
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsReimplementation(problems.next(), "List.of(Fruit.values())");
        assertEqualsReimplementation(problems.next(), "Set.of(Fruit.values())");
        assertEqualsReimplementation(problems.next(), "Set.of(Fruit.values())");
        assertEqualsReimplementation(problems.next(), "Arrays.copyOf(Fruit.values(), Fruit.values().length)");
        problems.assertExhausted();
    }

    @Test
    void testEnumValuesListingWithNull() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                enum Fruit {
                    APPLE, BANANA, CHERRY;
                }

                public class Test {
                    private static final Fruit[] FRUITS = { Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY, null };
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


