package de.firemage.autograder.core.check.oop;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLeakedCollectionCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.LEAKED_COLLECTION_RETURN,
        ProblemType.LEAKED_COLLECTION_ASSIGN
    );

    void assertEqualsLeakedReturn(Problem problem, String method, String field) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "leaked-collection-return",
                Map.of(
                    "method", method,
                    "field", field
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsLeakedAssign(Problem problem, String method, String field) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "leaked-collection-assign",
                Map.of(
                    "method", method,
                    "field", field
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsLeakedConstructor(Problem problem, String signature, String field) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "leaked-collection-constructor",
                Map.of(
                    "signature", signature,
                    "field", field
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testLeakedGetArrayInClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private int[] arrayA;
                    private String[][] arrayB;
                    
                    public int[] getA() {
                        return this.arrayA;
                    }
                    
                    public String[][] getB() {
                        return arrayB;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "getA", "arrayA");
        assertEqualsLeakedReturn(problems.next(), "getB", "arrayB");

        problems.assertExhausted();
    }

    @Test
    void testNoAssignment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                
                public class Test {
                    private List<String> list;
                    
                    public List<String> get() {
                        return list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMutableFieldInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list = new ArrayList<>();
                    
                    public List<String> get() {
                        return list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "get", "list");

        problems.assertExhausted();
    }

    @Test
    void testImmutableFieldInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list = List.of();
                    
                    public List<String> get() {
                        return list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConstructorMutableInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list;
                    
                    public Test() {
                        this.list = new ArrayList<>();
                    }

                    public List<String> get() {
                        return list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "get", "list");

        problems.assertExhausted();
    }

    @Test
    void testConstructorParamInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list;
                    
                    public Test(List<String> list) {
                        this.list = new ArrayList<>(list);
                    }

                    public List<String> get() {
                        return list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "get", "list");

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Type                    | Init                                                    ",
            " List<Integer>           | List.of()                                               ",
            " List<Integer>           | List.of(1, 2)                                           ",
            " List<Integer>           | Arrays.asList(new Integer[] {1, 2, 3})                  ",
            // everything from Collections that creates an immutable collection:
            " List<Integer>           | Collections.EMPTY_LIST                                  ",
            " Map                     | Collections.EMPTY_MAP                                   ",
            " Set                     | Collections.EMPTY_SET                                   ",
            " List                    | Collections.emptyList()                                 ",
            " Map                     | Collections.emptyMap()                                  ",
            " NavigableMap            | Collections.emptyNavigableMap()                         ",
            " NavigableSet            | Collections.emptyNavigableSet()                         ",
            " Set                     | Collections.emptySet()                                  ",
            " SortedMap               | Collections.emptySortedMap()                            ",
            " SortedSet               | Collections.emptySortedSet()                            ",
            " List<Integer>           | Collections.nCopies(5, 1)                               ",
            " Set<Integer>            | Collections.singleton(5)                                ",
            " List<Integer>           | Collections.singletonList(5)                            ",
            " Map<Integer, Integer>   | Collections.singletonMap(5, 4)                          ",

            " Collection<Integer>     | Collections.unmodifiableCollection(new ArrayList<>())   ",
            " List<Integer>           | Collections.unmodifiableList(new ArrayList<>())         ",
            " Map<Integer, Integer>   | Collections.unmodifiableMap(new HashMap<>())            ",
            " Map<Integer, Integer>   | Collections.unmodifiableNavigableMap(new TreeMap<>())   ",
            " Set<Integer>            | Collections.unmodifiableNavigableSet(new TreeSet<>())   ",

            " Set<Integer>            | Collections.unmodifiableSet(new TreeSet<>())            ",
            " Map<Integer, Integer>   | Collections.unmodifiableSortedMap(new TreeMap<>())      ",
            " Set<Integer>            | Collections.unmodifiableSortedSet(new TreeSet<>())      ",

            " Collection<Integer>     | Collections.unmodifiableCollection(new ArrayList<>())   ",
            " Collection<Integer>     | Collections.unmodifiableList(new ArrayList<>())         ",
        }
    )
    void testImmutableInit(String type, String init) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.*;

                public class Test {
                    private %s field = %s;

                    public %s get() {
                        return field;
                    }
                }
                """.formatted(type, init, type)
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testLambdaReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                import java.util.function.Supplier;

                public class Test {
                    private List<String> list = new ArrayList<>();

                    public Supplier<List<String>> getA() {
                        return () -> list;
                    }

                    public Supplier<List<String>> getB() {
                        return () -> { return list; };
                    }

                    public Supplier<List<String>> getC() {
                        return () -> { return List.copyOf(list); };
                    }
                }
                """
        ), PROBLEM_TYPES);

        // This is not implemented, because lambdas are rare.

        assertEqualsLeakedReturn(problems.next(), "lambda$0", "list");
        assertEqualsLeakedReturn(problems.next(), "lambda$1", "list");

        problems.assertExhausted();
    }

    @Test
    void testEnumArrayReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "FieldKind",
            """
                enum Vegetable { CARROT, SALAD; }

                public enum FieldKind {
                    FIELD(new Vegetable[] {Vegetable.CARROT, Vegetable.SALAD});

                    private final Vegetable[] possibleVegetables;

                    FieldKind(Vegetable[] possibleVegetables) {
                        this.possibleVegetables = possibleVegetables;
                    }

                    public Vegetable[] getPossibleVegetables() {
                        return this.possibleVegetables;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "getPossibleVegetables", "possibleVegetables");

        problems.assertExhausted();
    }

    @Test
    void testEnumListOf() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "FieldKind",
            """
                import java.util.List;

                enum Vegetable { CARROT, SALAD; }

                public enum FieldKind {
                    FIELD(List.of(Vegetable.CARROT, Vegetable.SALAD));

                    private final List<Vegetable> possibleVegetables;

                    FieldKind(List<Vegetable> possibleVegetables) {
                        this.possibleVegetables = possibleVegetables;
                    }

                    public List<Vegetable> getPossibleVegetables() {
                        return this.possibleVegetables;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testEnumMutableList() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "FieldKind",
            """
                import java.util.List;
                import java.util.ArrayList;

                enum Vegetable { CARROT, SALAD; }

                public enum FieldKind {
                    FIELD(new ArrayList<>(List.of(Vegetable.CARROT, Vegetable.SALAD)));

                    private final List<Vegetable> possibleVegetables;

                    FieldKind(List<Vegetable> possibleVegetables) {
                        this.possibleVegetables = possibleVegetables;
                    }

                    public List<Vegetable> getPossibleVegetables() {
                        return this.possibleVegetables;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "getPossibleVegetables", "possibleVegetables");

        problems.assertExhausted();
    }


    @Test
    void testAssignPublicConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list;
                    
                    public Test(List<String> list) {
                        this.list = list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedConstructor(problems.next(), "Test(List<String>)", "list");

        problems.assertExhausted();
    }

    @Test
    void testAssignMultiple() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> listA;
                    private List<String> listB;
                    
                    public Test(List<String> listA, List<String> listB) {
                        this.listA = listA;
                        this.listB = listB;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedConstructor(problems.next(), "Test(List<String>, List<String>)", "listA");
        assertEqualsLeakedConstructor(problems.next(), "Test(List<String>, List<String>)", "listB");

        problems.assertExhausted();
    }

    @Test
    void testSetter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list;
                    
                    public void setList(List<String> list) {
                        this.list = list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedAssign(problems.next(), "setList", "list");

        problems.assertExhausted();
    }

    @Test
    void testSetterArray() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private String[] array;
                    
                    public void setArray(String[] array) {
                        this.array = array;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedAssign(problems.next(), "setArray", "array");

        problems.assertExhausted();
    }

    @Test
    void testSetterCopy() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list;
                    
                    public void setList(List<String> list) {
                        this.list = new ArrayList<>(list);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAssignPublic() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    public List<String> list;
                    
                    public void setList(List<String> list) {
                        this.list = list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAssignNoWrite() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                class Test {
                    private List<String> list;
                    
                    void setList(List<String> list) {
                        this.list = list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedAssign(problems.next(), "setList", "list");

        problems.assertExhausted();
    }

    @Test
    void testAssignClassDefaultPublic() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                class Test {
                    private List<String> list = new ArrayList<>();
                    
                    void setList(List<String> list) {
                        this.list = list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedAssign(problems.next(), "setList", "list");

        problems.assertExhausted();
    }

    @Test
    void testAssignNotParameter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                public class Test {
                    private List<String> list;
                    
                    public void setList(List<String> list) {
                        List<String> otherList = new ArrayList<>();
                        this.list = otherList;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRecordNoConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Forest",
            """
                import java.util.List;
                import java.util.ArrayList;

                public record Forest(List<String> trees, List<String> animals) {}
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedConstructor(problems.next(), "Forest(List<String>, List<String>)", "trees");
        assertEqualsLeakedConstructor(problems.next(), "Forest(List<String>, List<String>)", "animals");

        assertEqualsLeakedReturn(problems.next(), "animals", "animals");
        assertEqualsLeakedReturn(problems.next(), "trees", "trees");

        problems.assertExhausted();
    }

    @Test
    void testRecordImmutable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Forest",
            """
                import java.util.List;
                import java.util.ArrayList;

                public record Forest(List<String> trees, List<String> animals) {
                    public Forest(List<String> trees, List<String> animals) {
                        this.trees = List.copyOf(trees);
                        this.animals = List.copyOf(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testRecordOverridden() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Forest",
            """
                import java.util.List;
                import java.util.ArrayList;

                public record Forest(List<String> trees, List<String> animals) {
                    public Forest(List<String> trees, List<String> animals) {
                        this.trees = new ArrayList<>(trees);
                        this.animals = new ArrayList<>(animals);
                    }
                    
                    @Override
                    public List<String> trees() {
                        return new ArrayList<>(trees);
                    }
                    
                    @Override
                    public List<String> animals() {
                        return new ArrayList<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRecordNotOverriddenGetter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Forest",
            """
                import java.util.List;
                import java.util.ArrayList;

                public record Forest(List<String> trees, List<String> animals) {
                    public Forest(List<String> trees, List<String> animals) {
                        this.trees = new ArrayList<>(trees);
                        this.animals = new ArrayList<>(animals);
                    }

                    public List<String> getTrees() {
                        return new ArrayList<>(trees);
                    }

                    public List<String> getAnimals() {
                        return new ArrayList<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "animals", "animals");
        assertEqualsLeakedReturn(problems.next(), "trees", "trees");

        problems.assertExhausted();
    }


    @Test
    void testRecordInitConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Forest",
            """
                import java.util.List;
                import java.util.ArrayList;

                public record Forest(List<String> trees, List<String> animals) {
                    public Forest(List<String> trees, List<String> animals) {
                        this.trees = new ArrayList<>(trees);
                        this.animals = animals;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "animals", "animals");
        assertEqualsLeakedReturn(problems.next(), "trees", "trees");
        assertEqualsLeakedConstructor(problems.next(), "Forest(List<String>, List<String>)", "animals");


        problems.assertExhausted();
    }

    @Test
    void testRecordUnmodifiableWithTempVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.Collections;
                import java.util.ArrayList;
                
                // example from https://stackoverflow.com/a/71486123
                public record Zoo(String name, Collection<String> animals) {
                     // NOTE: check that it overwrites the canonical constructor.
                     //       Overwriting other constructors does not guarantee immutability.
                    public Zoo(String name, Collection<String> animals) {
                        Collection<String> list = Collections.unmodifiableCollection(animals); // because it is unmodifiable, it must not overwrite the accessor
                        this.animals = list;
                        this.name = name;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testRecordCopy() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.ArrayList;

                public record Zoo(String name, Collection<String> animals) {
                    public Zoo(String name, Collection<String> animals) {
                        this.animals = new ArrayList<>(animals);
                        this.name = name;
                    }

                    public Collection<String> animals() {
                        return new ArrayList<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRecordCallingCanonicalConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.ArrayList;

                // example from https://stackoverflow.com/a/71486123
                public record Zoo(String name, Collection<String> animals) {
                    public Zoo(Collection<String> animals) {
                        this("DefaultZoo", animals); //# not ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedConstructor(problems.next(), "Zoo(String, Collection<String>)", "animals");
        assertEqualsLeakedReturn(problems.next(), "animals", "animals");

        problems.assertExhausted();
    }

    @Test
    void testRecordNotOverwritingAccessor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.ArrayList;
                
                // example from https://stackoverflow.com/a/71486123
                public record Zoo(String name, Collection<String> animals) {
                    // NOTE: check that it overwrites the canonical constructor.
                    //       Overwriting other constructors does not guarantee immutability.
                    public Zoo(String name, Collection<String> animals) {
                        Collection<String> list = new ArrayList<>(animals);
                        this.animals = list;
                        this.name = name;
                    }

                    //# not ok, accessor not overwritten
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "animals", "animals");

        problems.assertExhausted();
    }

    @Test
    void testConditionalReturn() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                class Test {
                    private List<String> list = new ArrayList<>();
                    
                    public List<String> get() {
                        if (list.isEmpty()) {
                            return new ArrayList<>();
                        } else {
                            return list;
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "get", "list");

        problems.assertExhausted();
    }


    @Test
    @Disabled("Not implemented")
    void testUnusedDefaultValue() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;
                
                class Test {
                    private List<String> list = new ArrayList<>(); // mutable, but always overwritten
                    
                    public Test() {
                        this.list = List.of();
                    }
                    
                    public List<String> get() {
                        return list;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testCompactConstructorImmutable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.ArrayList;

                public record Zoo(String name, Collection<String> animals) {
                    public Zoo {
                        animals = List.copyOf(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testCompactConstructorImmutableMap() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.Map;
                import java.util.Collection;
                import java.util.HashMap;

                public record Zoo(String name, Map<String, String> animals) {
                    public Zoo {
                        animals = Map.copyOf(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testCompactConstructorLeakedAssign() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.ArrayList;

                public record Zoo(String name, Collection<String> animals) {
                    public Zoo {
                        animals = animals;
                    }

                    public Collection<String> animals() {
                        return new ArrayList<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedConstructor(problems.next(), "Zoo(String, Collection<String>)", "animals");

        problems.assertExhausted();
    }

    @Test
    void testCompactConstructorLeakedAssignMap() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.Map;
                import java.util.Collection;
                import java.util.HashMap;

                public record Zoo(String name, Map<String, String> animals) {
                    public Zoo {
                        animals = animals;
                    }

                    public Map<String, String> animals() {
                        return new HashMap<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedConstructor(problems.next(), "Zoo(String, Map<String, String>)", "animals");

        problems.assertExhausted();
    }

    @Test
    void testCompactConstructorCopied() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.List;
                import java.util.Collection;
                import java.util.ArrayList;

                public record Zoo(String name, Collection<String> animals) {
                    public Zoo {
                        animals = new ArrayList<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "animals", "animals");

        problems.assertExhausted();
    }

    @Test
    void testCompactConstructorCopiedMap() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.Map;
                import java.util.Collection;
                import java.util.HashMap;

                public record Zoo(String name, Map<String, String> animals) {
                    public Zoo {
                        animals = new HashMap<>(animals);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsLeakedReturn(problems.next(), "animals", "animals");

        problems.assertExhausted();
    }


    @Test
    void testSelfAssignment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Zoo",
            """
                import java.util.Collection;

                public class Zoo {
                    private Collection<String> animals = null;

                    public Zoo() {
                        this.animals = animals;
                    }

                    public Collection<String> getAnimals() {
                        return this.animals;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


