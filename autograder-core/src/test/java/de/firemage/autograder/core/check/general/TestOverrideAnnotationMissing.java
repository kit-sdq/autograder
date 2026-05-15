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

class TestOverrideAnnotationMissing extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.OVERRIDE_ANNOTATION_MISSING);

    void assertMissingOverride(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "missing-override",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSuperclass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "A",
                    """
                    class A {
                        void a() {}
                        
                        void b() {}

                        String add(String a, String b) { return a + b; }

                        static void execute() {}
                    }
                    """
                ),
                Map.entry(
                    "B",
                    """
                    class B extends A {
                        void a() {} //# not ok
                        
                        @Override
                        void b() {} //# ok

                        String add(String a, String b, String c) { //# ok
                            return a + b + c;
                        }
                        
                        static void execute() {} //# ok
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "a");
        problems.assertExhausted();
    }

    @Test
    void testGeneralizationSpecialization() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "A",
                    """
                    abstract class A {
                        abstract Object a();

                        abstract String b(String value);

                        abstract void c() throws Exception;
                    }
                    """
                ),
                Map.entry(
                    "B",
                    """
                    abstract class B extends A {
                        String a() { return ""; } //# not ok

                        String b(Object value) { return ""; } //# ok

                        void c() throws RuntimeException {} //# not ok
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "a");
        assertMissingOverride(problems.next(), "c");
        problems.assertExhausted();
    }

    @Test
    void testOverrideThrows() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "A",
                    """
                    abstract class A {
                        abstract void a();
                    }
                    """
                ),
                Map.entry(
                    "B",
                    """
                    class B extends A {
                        void a() throws RuntimeException {} //# not ok
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "a");
        problems.assertExhausted();
    }

    @Test
    void testAbstractMethodHidesObjectMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Token",
            """
            public abstract class Token {
                public abstract String toString(); //# not ok
            }
            """
        ), PROBLEM_TYPES);

        // you can add an @Override annotation to that method
        assertMissingOverride(problems.next(), "toString");
        problems.assertExhausted();
    }

    @Test
    void testMissingOverrideOnInterface() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "RunnableImpl",
            """
            public class RunnableImpl implements Runnable {
                public void run() {} //# not ok
            }
            """
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "run");
        problems.assertExhausted();
    }

    @Test
    void testAnonymousClass() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "AnonymousClass",
            """
                public class AnonymousClass {
                    static {
                        new Thread(new Runnable() {
                            public void run() {
                                bar();
                            }

                            public void bar() {}
                        }).start();
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "run");
        problems.assertExhausted();
    }

    @Test
    void testEnum() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "MyEnum",
            """
                public enum MyEnum {
                    ONE {
                        public void foo() {}
                    },
                    TWO {
                        @Override
                        public void foo() {}
                    };
                
                    abstract void foo();
                }
                """
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "foo");
        problems.assertExhausted();
    }

    @Test
    void testEnumWithInterface() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "MyEnum",
            """
                public enum MyEnum implements Runnable {
                    ONE {
                        public void run() {}
                    },
                    TWO {
                        @Override
                        public void run() {}
                    };
                }
                """
        ), PROBLEM_TYPES);

        assertMissingOverride(problems.next(), "run");
        problems.assertExhausted();
    }
}


