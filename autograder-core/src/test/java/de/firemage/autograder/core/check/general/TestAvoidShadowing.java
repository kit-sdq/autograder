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

class TestAvoidShadowing extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "avoid-shadowing";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.AVOID_SHADOWING);

    private void assertEqualsHidden(String name, Problem problem) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of("name", name)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testHiddenUnusedParentField() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Parent",
                    """
                        public class Parent {
                            protected int number;
                            
                            @Override
                            public String toString() {
                                return "Parent(%d)".formatted(this.number);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Main",
                    """
                        public class Main extends Parent {
                            int number; /*# ok, because super.number is not used #*/

                            public Main() {
                                super();
                                this.number = 5;
                            }

                            public void doSomething() {
                                System.out.println(this.number);
                            }

                            public static void main(String[] args) {
                                Main main = new Main();
                                main.doSomething();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testHiddenParentField() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Parent",
                    """
                        public class Parent {
                            protected int number;
                            
                            @Override
                            public String toString() {
                                return "Parent(%d)".formatted(this.number);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Main",
                    """
                        public class Main extends Parent {
                            int number; /*# ok, because super.number is not used #*/

                            public Main() {
                                super();
                                this.number = 5;
                            }
                            
                            public void doSomething() {
                                // both fields are used:
                                System.out.println(this.number);
                                System.out.println(this.number);
                                System.out.println(super.number);
                                System.out.println(super.number);
                            }

                            public static void main(String[] args) {
                                Main main = new Main();
                                main.doSomething();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsHidden("number", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testLocalVariableHidesField() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            int number;

                            public Main() {
                                super();
                                this.number = 5;
                            }
                            
                            public void doSomething() {
                                int number = 6;

                                System.out.println(this.number);
                                System.out.println(this.number);
                                System.out.println(number);
                            }


                            public static void main(String[] args) {
                                Main main = new Main();
                                main.doSomething();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsHidden("number", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testLocalVariableHidesFields() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Parent",
                    """
                        public class Parent {
                            protected int number;
                            
                            @Override
                            public String toString() {
                                return "Parent(%d)".formatted(this.number);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Main",
                    """
                        public class Main extends Parent {
                            int number; /*# ok, because super.number is not used #*/

                            public Main() {
                                super();
                                this.number = 5;
                            }
                            
                            public void doSomething() {
                                int number = 6;

                                System.out.println(this.number);
                                System.out.println(this.number);
                                System.out.println(super.number);
                                System.out.println(super.number);
                                System.out.println(number);
                                System.out.println(number);
                            }

                            public static void main(String[] args) {
                                Main main = new Main();
                                main.doSomething();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsHidden("number", problems.next());
        assertEqualsHidden("number", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testStatic() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Parent",
                    """
                        public class Parent {
                            protected int number;
                            
                            @Override
                            public String toString() {
                                return "Parent(%d)".formatted(this.number);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Main",
                    """
                        public class Main extends Parent {
                            static int number;
                            
                            public void doSomething() {
                                int number = 6;

                                System.out.println(Main.number);
                                System.out.println(Main.number);
                                System.out.println(super.number);
                                System.out.println(super.number);
                                System.out.println(number);
                                System.out.println(number);
                            }

                            public static void main(String[] args) {
                                Main main = new Main();
                                main.doSomething();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsHidden("number", problems.next());
        assertEqualsHidden("number", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testInheritance() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "A",
                    """
                        class A {
                            protected int a;
                            int x;
                            static int y;
                            private int z;
                        }
                        """
                ),
                Map.entry(
                    "B",
                    """
                        class B extends A {
                            private int b;
                            private final int c;

                            public B(int b, int c) {
                                this.b = b;
                                this.c = c;
                                
                                System.out.println(b);
                                System.out.println(b);
                                System.out.println(this.b);
                                System.out.println(this.b);
                                System.out.println(c);
                                System.out.println(c);
                                System.out.println(this.c);
                                System.out.println(this.c);
                            }

                            public void setB(int b) {
                                this.b = b;
                            }

                            private void foo2(int b) {}
                            private void foo() {
                                int a = 3; /*# not ok #*/
                                int x = 4; /*# not ok #*/
                                int y = 5; /*# not ok #*/
                                final int z = 5; /*# ok #*/
                                
                                System.out.println("" + a + this.a);
                                System.out.println("" + a + this.a);
                                System.out.println("" + x + this.x);
                                System.out.println("" + x + this.x);
                                System.out.println("" + y + A.y);
                                System.out.println("" + y + A.y);
                                System.out.println("" + z);
                                System.out.println("" + z);
                            }
                        }
                        """
                ),
                Map.entry(
                    "C",
                    """
                        class C extends A {
                            protected int a; /*# not ok #*/
                            int x; /*# not ok #*/
                            static int y; /*# not ok #*/
                            private int z; /*# ok #*/
                            
                            void doSomething() {
                                System.out.println("" + super.a + this.a);
                                System.out.println("" + super.a + this.a);
                                System.out.println("" + super.x + this.x);
                                System.out.println("" + super.x + this.x);
                                System.out.println("" + A.y + C.y);
                                System.out.println("" + A.y + C.y);
                                System.out.println("" + z);
                                System.out.println("" + z);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsHidden("a", problems.next());
        assertEqualsHidden("x", problems.next());
        assertEqualsHidden("y", problems.next());

        assertEqualsHidden("a", problems.next());
        assertEqualsHidden("x", problems.next());
        assertEqualsHidden("y", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testException() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "SomeException",
                    """
                        class SomeException extends IllegalArgumentException {
                            @java.io.Serial
                            private static final long serialVersionUID = -4491591333105161142L; /*# ok #*/

                            public SomeException(String message) {
                                super(message + serialVersionUID);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testInStaticContext() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        class Main {
                            private String string;

                            public static void doSomething(String string) {
                                System.out.println(string);
                            }
                            
                            @Override
                            public String toString() {
                                return this.string;
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testArrayParamHidesAttribute() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        class Main {
                            private String ais;

                            public void doSomething(String[] ais) {
                                System.out.println(ais);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        // not reported, because only the array is used in the method

        problems.assertExhausted();
    }
}


