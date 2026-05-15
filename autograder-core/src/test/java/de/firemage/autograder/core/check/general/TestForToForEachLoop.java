package de.firemage.autograder.core.check.general;

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

class TestForToForEachLoop extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.FOR_CAN_BE_FOREACH);

    void assertEqualsForEach(Problem problem, String type, String iterable) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "common-reimplementation",
                Map.of("suggestion", "for (%s value : %s) { ... }".formatted(type, iterable))
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testArray() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    public static void main(String[] args) {
                        //# not ok
                        for (int i = 0; i < args.length; i++) {
                            System.out.println(args[i]);
                        }

                        //# ok
                        for (int i = 0; i < args.length; i++) {
                            args[i] = "Hello World";

                            System.out.println(args[i]);
                        }
                        
                        int[][] doubleArray = new int[10][10];
                        
                        //# not ok
                        for (int i = 0; i < doubleArray.length; i++) {
                            System.out.println(doubleArray[i]);
                        }

                        //# ok
                        for (int i = 0; i < doubleArray.length; i++) {
                            doubleArray[i][i] = 1;

                            System.out.println(doubleArray[i]);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsForEach(problems.next(), "String", "args");
        assertEqualsForEach(problems.next(), "int[]", "doubleArray");

        problems.assertExhausted();
    }

    @Test
    void testList() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;

                class Test {
                    public static void main(String[] args) {
                        List<Integer> list = List.of(1, 2, 3, 4, 5, 6);

                        for (int i = 0; i < list.size(); i++) { //# not ok
                            System.out.println(list.get(i));
                        }

                        for (int i = 0; i < list.size(); i++) { //# not ok
                            System.out.println(list.get(i));
                            System.out.println(list.get(i));
                        }

                        for (int i = 0; i < list.size(); i += 2) { //# ok
                            System.out.println(list.get(i));
                        }

                        for (int i = 0; i < list.size() - 1; i += 2) { //# ok
                            System.out.println(list.get(i));
                        }

                        for (int i = 0; i < list.size(); i++) { //# ok
                            System.out.println(i);
                        }

                        for (int i : list) { //# ok
                            System.out.println(i);
                        }
                    }
                    
                    void lessEqualCondition(List<String> lo) {
                        for (int i = 0; i <= lo.size() - 1; i++) { /*# not ok #*/
                            System.out.println(lo.get(i));
                        }
                    }
                    
                    void concreteList(ArrayList<String> arrayList) {
                        for (int i = 0; i < arrayList.size(); i++) { /*# not ok #*/
                            System.out.println(arrayList.get(i));
                        }
                    }
                    
                    void listWithIndexUse(List<String> withIndexUse) {
                        for (int i = 0; i < withIndexUse.size(); i++) { /*# ok #*/
                            System.out.println(i + ": " + withIndexUse.get(i));
                        }
                    }
                    
                    
                    protected static final char[] filter(char[] chars, char removeChar) {
                        int count = 0;
                        for (int i = 0; i < chars.length; i++) { /*# not ok #*/
                            if (chars[i] == removeChar) {
                                count++;
                            }
                        }

                        char[] results = new char[chars.length - count];

                        int index = 0;
                        for (int i = 0; i < chars.length; i++) { /*# not ok #*/
                            if (chars[i] != removeChar) {
                                results[index++] = chars[i];
                            }
                        }
                        return results;
                    }
                    
                    private void fofo(List<Foo> mList) {
                        for (int i = 0; i < mList.size(); i++) { /*# ok #*/
                            mList.get(i).setIndex(i);
                        }
                    }

                    interface Foo {
                        void setIndex(int i);
                    }
                    
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsForEach(problems.next(), "int", "list");
        assertEqualsForEach(problems.next(), "int", "list");
        assertEqualsForEach(problems.next(), "String", "lo");
        assertEqualsForEach(problems.next(), "String", "arrayList");
        assertEqualsForEach(problems.next(), "char", "chars");
        assertEqualsForEach(problems.next(), "char", "chars");

        problems.assertExhausted();
    }

    @Test
    void testReverseAccess() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                
                class Test {
                    void loop(List<String> l) {
                        for (int i = l.size() - 1; i > 0; i-= 1) { /*# ok #*/
                            System.out.println(i + ": " + l.get(i));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIndexInitOutsideLoop() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                
                class Test {
                    void loop(List<String> l) {
                        int i = 0;
                        for (; i < l.size(); i++) { /*# not ok #*/
                            System.out.println(l.get(i));
                        }
                    }

                    void loop2(List<String> usedAfterLoop) {
                        int i = 0;
                        for (; i < usedAfterLoop.size(); i++) { /*# ok; i is used after loop #*/
                            System.out.println(usedAfterLoop.get(i));

                            if (usedAfterLoop.get(i).equals("foo")) {
                                break;
                            }
                        }

                        System.out.println(usedAfterLoop.get(i));
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsForEach(problems.next(), "String", "l");

        problems.assertExhausted();
    }

    @Test
    void testIndexNotZero() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                
                class Test {
                    void loop(List<String> filters, StringBuilder builder) {
                        for (int i = 1; i < filters.size(); i++) { /*# ok #*/
                            builder.append(' ');
                            builder.append(filters.get(i));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAccessNextElement() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    static String findOptionalStringValue(String[] args, String name, String defaultValue) {
                        for (int i = 0; i < args.length; i++) { /*# ok #*/
                            if (args[i].equals(name)) {
                                return args[i + 1];
                            }
                        }
                        return defaultValue;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testOnlyArrayWrite() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    private String newString() {
                        int strLength = randomInt(1, 100);

                        char[] chars = new char[strLength];
                        for (int i = 0; i < chars.length; i++) { /*# ok #*/
                            chars[i] = randomCharIn("123");
                        }
                        return new String(chars);
                    }

                    private int randomInt(int min, int max) {
                        return 42;
                    }

                    private char randomCharIn(String s) {
                        return '1';
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testTransferListIndexToArrayIndex() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;

                class Test {
                    private int[] hashes = new int[10];
                    
                    public void foo() {
                        List<String> stringList = new ArrayList<>();

                        this.hashes = new int[stringList.size()];
                        for (int i = 0; i < stringList.size(); i++) { /*# ok #*/
                            this.hashes[i] = stringList.get(i).hashCode();
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testIndexSameArrayFieldDifferentInstances() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    final int hashes[] = new int[6];

                    public boolean equals(Test other) {
                        for (int i = 0; i < hashes.length; i++) { /*# ok #*/
                            if (this.hashes[i] != other.hashes[i])
                                return false;
                        }
                        
                        return true;
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testLoopWithDifferentListInCondition() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.ArrayList;

                class Test {
                    void loop(List<String> l) {
                        List<String> l2 = new ArrayList<>(l);
                        for (int i = 0; i < l.size(); i++) { /*# ok #*/
                            System.out.println(l2.get(i));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testGenerics() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;

                class Test {
                    <T> void loop(List<T> list) {
                        for (int i = 0; i < list.size(); i++) {
                            System.out.println(list.get(i));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsForEach(problems.next(), "T", "list");

        problems.assertExhausted();
    }

    @Test
    void testFieldAccess() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Node",
            """
                class Node {
                    Node[] children;

                    public Object childrenAccept(Object data) {
                        if (children != null) {
                            for (int i = 0; i < children.length; ++i) { /*# not ok #*/
                                Node apexNode = (Node) children[i];
                                System.out.println(apexNode);
                            }
                        }
                        return data;
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsForEach(problems.next(), "Node", "children");

        problems.assertExhausted();
    }

    @Test
    void testMutatingCollection() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Main",
            """
                import java.util.ArrayList;
                import java.util.List;
                
                public class Main {
                    public static void main(String[] args) {
                        var list = new ArrayList<>(List.of("a", "toRemove", "b", "toRemove"));
                        \s
                        for (int i = 0; i < list.size(); i++) {
                            var e = list.get(i);
                            if (e.equals("toRemove")) {
                                list.remove(e);
                            }
                        }
                        \s
                        System.out.println(list);
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


