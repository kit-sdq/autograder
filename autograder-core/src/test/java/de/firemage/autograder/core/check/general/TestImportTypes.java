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

class TestImportTypes extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.IMPORT_TYPES);

    void assertEqualsImport(String typeName, Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "import-types",
                Map.of("type", typeName)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testQualifiedSimpleVariableType() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    private Test test; //# ok
                    private java.lang.Integer integer; //# not ok

                    public static void main(String[] args) {
                        java.lang.String string = "Hello World"; //# not ok; import java.lang.String
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsImport("java.lang.Integer", problems.next());
        assertEqualsImport("java.lang.String", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testQualifiedNestedVariableType() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Map;

                class Test {
                    static class A { static class B {} }

                    void testKnown() {
                        A a = new A(); /*# ok #*/
                        A.B b = new A.B(); /*# ok #*/
                    }

                    void testForeign() {
                        Map.Entry<String, String> entry = null; /*# ok #*/
                        java.util.Map.Entry<String, String> entry2 = null; /*# not ok #*/
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsImport("java.util.Map", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testQualifiedArrays() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Arrays;

                class Test {
                    private java.util.Scanner[] scanners; /*# not ok; import java.util.Scanner #*/
                    private java.lang.Double[][] doubles; /*# not ok; import java.lang.Double #*/

                    Test(String... lines) { /*# ok #*/
                        var stream = Arrays.stream(lines); /*# ok #*/
                    }

                    Test(java.lang.Integer... values) { /*# not ok #*/
                    }

                    private void call(String... varargs) { /*# ok #*/
                        takesArray(new String[] {"Hello", "World"}); /*# ok #*/
                        String[] array = varargs; /*# ok #*/
                        takesArray(array); /*# ok #*/
                        takesArray(varargs); /*# ok; there is an implicit cast to Object[] #*/
                    }

                    private <T> void takesArray(T[] array) {} /*# ok #*/

                    private void foo(String... varargs) {} /*# ok #*/
                    private void bar(java.lang.String... varargs) {}  /*# not ok; import java.lang.String #*/
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsImport("java.util.Scanner", problems.next());
        assertEqualsImport("java.lang.Double", problems.next());
        assertEqualsImport("java.lang.Integer", problems.next());
        assertEqualsImport("java.lang.String", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testNestedGenericQualifiedTypes() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.Map;

                class Test {
                    private List<java.lang.Integer> integers; //# not ok
                    private Map<java.lang.String, java.util.List<java.lang.Double>> map; //# not ok
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsImport("java.lang.Integer", problems.next());
        assertEqualsImport("java.lang.String", problems.next());
        assertEqualsImport("java.util.List", problems.next());
        assertEqualsImport("java.lang.Double", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testExtraArraySyntax() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.List;
                import java.util.Arrays;

                class Test {
                    public static <T> String[] takesList(List<T> list) {
                        return list.stream().map(Object::toString).toArray(String[]::new); //# ok
                    }

                    public static String[] makeCopy(String[] array) {
                        System.out.println(Integer.class); //# ok
                        return Arrays.copyOf(array, array.length, String[].class); //# ok
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testForEach() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Map;

                class Test {
                    public static void takesMap(Map<Integer, String[][]> input, Map<Integer, Boolean> output) {
                        input.forEach((key, strings) -> output.put(key, false));
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


