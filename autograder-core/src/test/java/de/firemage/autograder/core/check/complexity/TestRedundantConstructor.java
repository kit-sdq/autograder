package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.SourceInfo;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestRedundantConstructor extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REDUNDANT_DEFAULT_CONSTRUCTOR);

    @Test
    void testRedundantConstructor() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                public class Test {
                    public Test() {
                    
                    }
                }
                """
        );
    }

    @Test
    void testRedundantHigherVisibility() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                class Test {
                    public Test() {
                    }
                }
                """
        );
    }

    @Test
    void testRedundantHigherVisibilityInner() throws IOException, LinterException {
        assertRedundant(
            "Outer",
            """
                public class Outer {
                    private class Inner {
                        Inner() {}
                    }
                }
                """
        );
    }

    @Test
    void testRedundantExplicitSuper() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                public class Test {
                    public Test() {
                        super();
                    }
                }
                """
        );
    }

    @Test
    void testRedundantConstructorInnerClassSuper() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                public class Test {
                    public class Inner {
                        public Inner() {
                            super(/* test */);
                        }
                    }
                }
                """
        );
    }

    @Test
    void testRedundantConstructorStaticInner() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                public class Test {
                    private static class Inner {
                        private Inner() {
                        }
                    }
                }
                """
        );
    }

    @Test
    void testRedundantEnumConstructor() throws IOException, LinterException {
        assertRedundant(
            "TestEnum",
            """
                public enum TestEnum {
                    ;
                    private TestEnum() {
                    }
                }
                """
        );
    }

    @Test
    void testRedundantEnumConstructorNoModifier() throws IOException, LinterException {
        assertRedundant(
            "TestEnum",
            """
                public enum TestEnum {
                    ;
                    TestEnum() {
                    }
                }
                """
        );
    }

    @Test
    void testRedundantRecordCompactConstructor() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                record Test() {
                    Test {
                    }
                }
                """
        );
    }

    @Test
    void testRedundantEmptyRecordNormalConstructor() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                public record Test() {
                    public Test() {
                    }
                }
                """
        );
    }

    @Test
    void testRedundantRecordNormalConstructor() throws IOException, LinterException {
        assertRedundant(
            "Test",
            """
                public record Test(int a, int b) {
                    public Test(int a, int b) {
                        this.b = b;
                        this.a = a;
                    }
                }
                """
        );
    }

    @Test
    void testNotRedundantImplicitConstructor() throws IOException, LinterException {
        assertNotRedundant(
            "Test",
            """
                public class Test {
                }
                """
        );
    }

    @Test
    void testNotRedundantImplicitRecordConstructor() throws IOException, LinterException {
        assertNotRedundant(
            "TestRecord",
            """
                record TestRecord() {
                }
                """
        );
    }

    @Test
    void testNotRedundantRecordNormalConstructor() throws IOException, LinterException {
        assertNotRedundant(
            "Test",
            """
                public record Test(int a, int b) {
                    public Test(int a, int b) {
                        this.a = b;
                        this.b = a;
                    }
                }
                """
        );
    }

    @Test
    void testNotRedundantRecordNormalConstructor2() throws IOException, LinterException {
        assertNotRedundant(
            "Test",
            """
                public record Test(int a, int b) {
                    public Test(int a, int b) {
                        this.a = a;
                        this.b = b;
                        System.out.println();
                    }
                }
                """
        );
    }

    @Test
    void testNotRedundantConstructorVisibility() throws IOException, LinterException {
        assertNotRedundant(
            "Test",
            """
                public class Test {
                    protected Test() {
                    }
                }
                """
        );
    }

    @Test
    void testNotRedundantBody() throws IOException, LinterException {
        assertNotRedundant(
            "Test",
            """
                public class Test {
                    public Test() {
                        System.out.println();
                    }
                }
                """
        );
    }

    @Test
    void testNotRedundantThrows() throws IOException, LinterException {
        assertNotRedundant(
            "Test",
            """
                public class Test {
                    public Test() throws java.io.IOException {
                    }
                }
                """
        );
    }

    @Test
    void testNotRedundantConstructorSuperArgs() throws IOException, LinterException {
            assertRedundant(
            StringSourceInfo.fromSourceStrings(Map.of(
                "Base",
                """
                    public class Base {
                        public Base(String foo) {
                        }
                    }
                    """,
                "Child",
                """
                    public class Child extends Base {
                        public Child() {
                            super("foo");
                        }
                    }
                    """
            )),
            false
        );
    }

    @Test
    void testNotRedundantQualifiedSuper() throws IOException, LinterException {
        // taken from JLS example 8.8.7.1-2
            assertRedundant(
            StringSourceInfo.fromSourceStrings(Map.of(
                "Outer",
                """
                    class Outer {
                        class Inner {}
                    }
                    """,
                "Child",
                """
                    class Child extends Outer.Inner {
                        Child() {
                            (new Outer()).super();
                        }
                    }
                    """
            )),
            false
        );
    }

    @Test
    void testNotRedundantConstructorProtectedInnerClass() throws IOException, LinterException {
        // Taken from JLS example 8.8.9-2
            assertRedundant(
            StringSourceInfo.fromSourceStrings(Map.of(
                "a.Outer",
                """
                    package a;
                    public class Outer {
                        protected class Inner {
                            public Inner() {}
                        }
                    }
                    """,
                "b.Child",
                """
                    package b;
                    public class Child extends a.Outer {
                        void foo() {
                            new Inner();
                        }
                    }
                    """
            )),
            false
        );
    }

    private void assertRedundant(String className, String source) throws LinterException, IOException {
        assertRedundant(className, source, true);
    }

    private void assertNotRedundant(String className, String source) throws LinterException, IOException {
        assertRedundant(className, source, false);
    }

    private void assertRedundant(String className, String source, boolean redundant) throws LinterException, IOException {
        assertRedundant(StringSourceInfo.fromSourceString(className, source), redundant);
    }

    private void assertRedundant(SourceInfo info, boolean redundant) throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(info, PROBLEM_TYPES);
        if (redundant) {
            assertEqualsRedundant(problems.next());
        }
        problems.assertExhausted();
    }

    private void assertEqualsRedundant(Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage("implicit-constructor-exp")),
            this.linter.translateMessage(problem.getExplanation())
        );
    }
}
