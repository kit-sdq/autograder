package de.firemage.autograder.core.check.oop;

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

class TestConcreteCollectionCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.CONCRETE_COLLECTION_AS_FIELD_OR_RETURN_VALUE);

    void assertEqualsConcrete(Problem problem, String type) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "concrete-collection",
                Map.of(
                    "type", type
                )
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testLocalAssignment() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Collection;
                import java.util.HashSet;
                import java.util.Set;

                public class Test {
                    public static void main(String[] args) {
                        ArrayList<String> list = new ArrayList<>(); /*# not ok #*/

                        List<Integer> list2 = new ArrayList<>(); /*# ok #*/

                        Collection<Integer> list3 = new ArrayList<>(); /*# ok #*/

                        HashSet<Integer> set = new HashSet<>(); /*# not ok #*/

                        Set<Integer> set2 = new HashSet<>(); /*# ok #*/
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsConcrete(problems.next(), "ArrayList<String>");
        assertEqualsConcrete(problems.next(), "HashSet<Integer>");

        problems.assertExhausted();
    }

    @Test
    void testNestedTypes() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Collection;
                import java.util.HashSet;
                import java.util.Set;
                import java.util.HashMap;
                import java.util.Map;

                public class Test {
                    public static void main(String[] args) {
                        List<List<String>> a = new ArrayList<>(); /*# ok #*/
                        Map<String, List<String>> b = new HashMap<>(); /*# ok #*/

                        List<HashSet<String>> c = new ArrayList<>(); /*# not ok #*/
                        HashMap<String, ArrayList<String>> d = new HashMap<>(); /*# not ok #*/
                        Map<String, ArrayList<String>> e = new HashMap<>(); /*# not ok #*/
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsConcrete(problems.next(), "HashSet<String>");
        assertEqualsConcrete(problems.next(), "HashMap<String, ArrayList<String>>");
        assertEqualsConcrete(problems.next(), "ArrayList<String>");

        problems.assertExhausted();
    }

    @Test
    void testInArray() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Stack;
                import java.util.Map;
                import java.util.HashMap;

                public class Test {
                    private Stack<Object>[][] foo; /*# not ok #*/

                    public static void main(String[] args) {
                        List[] array = new ArrayList[10]; /*# ok #*/

                        ArrayList<String>[] array2; /*# not ok #*/
                        array[0] = new ArrayList<>(); /*# ok #*/
                    }

                    void bar() {
                        int x = foo[0][0].size();
                    }

                    private static void doB(Map<Integer, String>[] map) {}

                    private static void doA() {
                        HashMap<Integer, String>[] /*# not ok #*/ map = new HashMap[] {};

                        doB(map);
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsConcrete(problems.next(), "Stack<Object>");
        assertEqualsConcrete(problems.next(), "ArrayList<String>");
        assertEqualsConcrete(problems.next(), "HashMap<Integer, String>");

        problems.assertExhausted();
    }

    @Test
    void testAllowedContext() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.AbstractMap;
                import java.util.HashMap;
                import java.util.Collection;
                import java.util.Set;

                public class Test {
                    // false positive with method reference:
                    private static final ThreadLocal TREE_CACHE =
                        ThreadLocal.withInitial(HashMap::new); /*# ok #*/

                    final Object literal = ArrayList.class; /*# ok #*/
                    final Object o2 =
                        new AbstractMap.SimpleEntry<>("", ""); /*# ok #*/

                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>();

                        if (list instanceof ArrayList<Integer>) { /*# ok #*/
                            var list2c = (ArrayList<Integer>) list; /*# ok #*/
                            System.out.println("Ok");
                        }

                        if (list.getClass().equals(ArrayList.class)) { /*# ok #*/
                            System.out.println("Ok");
                        }

                        var someList = new ArrayList<>(); /*# ok #*/
                        AbstractMap.SimpleEntry<String, String> entry = null; /*# ok #*/
                    }

                    static class O3 extends ArrayList {
                        {
                            O3.super.clear(); /*# ok #*/
                        }

                        class Inner {
                            {
                                O3.this.clear(); /*# ok #*/
                                O3.super.clear(); /*# ok #*/
                            }
                        }
                    }
                }
                

                final class Foo3<A, B extends Collection<A>> { /*# ok #*/
                    private Set<? super B> things; /*# ok #*/
                    class Vector { Vector(String a, String b, int c) {} }

                    private Vector vec = new Vector("a", "b", 1); /*# ok #*/
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConcreteCollectionInTypes() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.TreeMap;

                class MyList extends ArrayList<String> {} /*# ok #*/

                abstract class A {
                    abstract ArrayList<String> getList(); /*# not ok #*/
                }

                class B extends A {
                    @Override
                    ArrayList<String> getList() { /*# ok #*/
                        return new ArrayList<>();
                    }
                }

                record MyRecord(ArrayList<String> list) {} /*# not ok #*/

                public class Test {
                    private TreeMap<Integer, String> treeMap; /*# not ok #*/
                    
                    public Test() {
                        this.treeMap = null; /*# ok #*/
                    }
                }

                class TestThisAssignment {
                    private ArrayList<String>[] /*# not ok #*/ list;

                    public void init(int count) {
                        this.list = new ArrayList[count] /*# ok #*/;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsConcrete(problems.next(), "ArrayList<String>");
        assertEqualsConcrete(problems.next(), "ArrayList<String>");
        assertEqualsConcrete(problems.next(), "TreeMap<Integer, String>");
        assertEqualsConcrete(problems.next(), "ArrayList<String>");

        problems.assertExhausted();
    }

    @Test
    void testRawTypes() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "TestRawTypes",
            """
                import java.util.HashSet;
                import java.util.Set;
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Vector;
                import java.util.Map;
                import java.util.HashMap;
                import java.util.Properties;

                public class TestRawTypes {
                    Set attr1 = new HashSet(); /*# ok #*/

                    HashSet attr2 = new HashSet(); /*# not ok #*/
                    HashMap attr3 = new HashMap(); /*# not ok #*/

                    HashSet foo() { /*# not ok #*/
                        return new HashSet();
                    }

                    Map getFoo() { /*# ok #*/
                        return new HashMap();
                    }

                    Set foo4() { /*# ok #*/
                        return new HashSet();
                    }

                    void foo5(HashMap bar) {} /*# not ok #*/

                    void foo6(Vector bar) {} /*# not ok #*/

                    void foo7(ArrayList bar) {} /*# not ok #*/

                    // with instanceof and cast
                    boolean m(Map m) {
                        if (m instanceof HashMap) { /*# ok #*/
                            return ((HashMap) m).isEmpty(); /*# ok #*/
                        }
                        return false;
                    }

                    static class MyMap extends HashMap implements Map { /*# ok #*/
                        static Map create() { return null; }
                    }

                    static class FooInner {
                        final Map map1 =
                            MyMap.create(); /*# ok #*/
                        final Map[] map2 = new MyMap[5]; /*# ok #*/
                        final Properties map3 = new Properties(); /*# ok #*/
                    }

                    static class O extends ArrayList implements List {
                        final O map = new O(); /*# not ok #*/
                    }
                    
                    @SuppressWarnings("rawtypes")
                    class Foo2 {
                        void myMethod() {
                            class Inner {
                                HashSet foo() { /*# not ok #*/
                                    return new HashSet();
                                }
                            }
                            Object o = new Object() {
                                HashSet foo() { return new HashSet(); } /*# not ok #*/
                            };
                        }

                        class Nested {
                            HashSet foo() { /*# not ok #*/
                                return new HashSet();
                            }
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsConcrete(problems.next(), "HashSet");
        assertEqualsConcrete(problems.next(), "HashMap");
        assertEqualsConcrete(problems.next(), "HashSet");
        assertEqualsConcrete(problems.next(), "HashMap");
        assertEqualsConcrete(problems.next(), "Vector");
        assertEqualsConcrete(problems.next(), "ArrayList");
        assertEqualsConcrete(problems.next(), "TestRawTypes.O");
        assertEqualsConcrete(problems.next(), "HashSet");
        assertEqualsConcrete(problems.next(), "HashSet");
        assertEqualsConcrete(problems.next(), "HashSet");

        problems.assertExhausted();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Type              ",
            " LinkedHashSet     ",
            " LinkedHashMap     ",
            " EnumMap           ",
            " EnumSet           ",
        }
    )
    void testSequencedCollection(String type) throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                import java.util.*;

                public class Test {
                    private %s collection;
                }
                """.formatted(type)
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
