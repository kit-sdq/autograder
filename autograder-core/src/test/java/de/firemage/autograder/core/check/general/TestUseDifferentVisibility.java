package de.firemage.autograder.core.check.general;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.api.JavaVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseDifferentVisibility extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.USE_DIFFERENT_VISIBILITY,
        ProblemType.USE_DIFFERENT_VISIBILITY_PEDANTIC,
        ProblemType.USE_DIFFERENT_VISIBILITY_PUBLIC_FIELD
    );

    void assertDifferentVisibility(Problem problem, String name, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "use-different-visibility",
                Map.of("name", name, "suggestion", suggestion)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertDifferentVisibilityField(Problem problem, String name, String suggestion) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "use-different-visibility-field",
                Map.of("name", name, "suggestion", suggestion)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testNoOtherReferences() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        public class Example {
                            void foo() {}

                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);


        problems.assertExhausted();
    }

    @Test
    void testPackagePrivateRoot() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        public class Example {
                            void example() {}
                            
                            public static void main(String[] args) {}
                        }
                        """
                ),
                Map.entry(
                    "Other",
                    """
                        public class Other {
                            private static void foo() {
                                Example example = new Example();
                                example.example();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);


        problems.assertExhausted();
    }

    @Test
    void testPackagePrivateDifferentPackage() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "com.Example",
                    """
                        package com;
                        public class Example {
                            public String exampleVariable;
                            
                            public static void main(String[] args) {}
                        }
                        """
                ),
                Map.entry(
                    "Other",
                    """
                        import com.Example;

                        public class Other {
                            private static void foo() {
                                Example example = new Example();
                                example.exampleVariable = "foo";
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertDifferentVisibilityField(problems.next(), "exampleVariable", "private");

        problems.assertExhausted();
    }

    @Test
    void testPrivate() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        public class Example {
                            public String exampleVariable;

                            private void foo() {
                                exampleVariable = "foo";
                            }
                            
                            static class Inner {
                                private void bar() {
                                    Example example = new Example();
                                    example.exampleVariable = "bar";
                                }
                            }
                            
                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertDifferentVisibility(problems.next(), "exampleVariable", "private");

        problems.assertExhausted();
    }

    @Test
    void testPrivateNestedClass() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        public class Example {

                            private void foo() {
                                Inner inner = new Inner();
                                inner.exampleVariable = "foo";
                            }
                            
                            static class Inner {
                                public String exampleVariable;
                            }
                            
                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);


        assertDifferentVisibility(problems.next(), "exampleVariable", "private");

        problems.assertExhausted();
    }

    @Test
    void testMainMethod() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);


        problems.assertExhausted();
    }

    @Test
    void testMethodVisibility() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public void foo() {} // Not Ok
                            
                            void bar() {} // Not Ok
                            
                            private void baz() {} // Ok
                            
                            void a() {} // Not Ok

                            private void b() {
                                a();
                            }
                            
                            public void c() {} // Not Ok
                            
                            void d() {} // Ok
                            
                            void e() {
                                // so that all methods are used
                                foo();
                                bar();
                                baz();
                                a();
                                b();
                                c();
                                d();
                            }
                            
                            public static void main(String[] args) {}
                        }
                        """
                ),
                Map.entry(
                    "Other",
                    """
                        public class Other {
                            private void call() {
                                Main main = new Main();
                                main.c();
                                main.d();
                                main.e();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("foo", "private");
        expected.put("bar", "private");
        expected.put("a", "private");
        expected.put("c", "default");

        int i = 0;
        for (var entry : expected.entrySet()) {
            assertDifferentVisibility(problems.next(), entry.getKey(), entry.getValue());
            i += 1;
        }

        problems.assertExhausted();
    }

    @Test
    void testOverriddenMethod() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            @Override
                            public boolean equals(Object other) {
                                return true;
                            }
                            
                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testBackwardReference() throws LinterException, IOException {
        // The checkstyle enforces that constants should be sorted by their visibility, so
        // for example, public variables should be before private variables.
        //
        // In the example below, both constants have default visibility, but `DATE_FORMAT` could
        // be private. However, since `DATE_FORMAT2` references `DATE_FORMAT`, according to the
        // checkstyle it would have to be declared before `DATE_FORMAT`, which is not possible:
        //
        // static final String DATE_FORMAT2 = DATE_FORMAT + " HH:mm:ss";
        // private static final String DATE_FORMAT = "yyyy-MM-dd";
        //
        // ^ this will not compile
        //
        // The solution: Constants must not have a lower visibility than the constants (in the same class)
        //               they are referenced in.
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            static final String DATE_FORMAT = "yyyy-MM-dd";
                            static final String DATE_FORMAT2 = DATE_FORMAT + " HH:mm:ss";

                            public static void main(String[] args) {}
                        }
                        """
                ),
                Map.entry(
                    "Other",
                    """
                        public class Other {
                            public static void main(String[] args) {
                                System.out.println(Main.DATE_FORMAT2);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testBackwardReferenceCanBeLowered() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            static final String DATE_FORMAT = "yyyy-MM-dd";
                            private static final String DATE_FORMAT2 = DATE_FORMAT + " HH:mm:ss";

                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertDifferentVisibility(problems.next(), "DATE_FORMAT", "private");
        problems.assertExhausted();
    }

    @Test
    void testVisibilityProtected() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "foo.Parent",
                    """
                        package foo;

                        public class Parent {
                            protected static final String SOME_VAR = "foo";
                        }
                        """
                ),
                Map.entry(
                    "Child",
                    """
                        import foo.Parent;

                        public class Child extends Parent {
                            public static void main(String[] args) {
                                System.out.println(Parent.SOME_VAR);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testImplementedProtectedMethod() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "ui.Command",
                    """
                        package ui;

                        public abstract class Command {
                            protected static final String SOME_VAR = "foo";
                            
                            public void execute() {
                                executePlatform();
                            }
                            
                            protected abstract void executePlatform();
                        }
                        """
                ),
                Map.entry(
                    "ui.commands.ExampleCommand",
                    """
                        package ui.commands;
                                                
                        import ui.Command;

                        public class ExampleCommand extends Command {
                            @Override
                            protected void executePlatform() {}
                        }
                        """
                ),
                Map.entry(
                    "Main",
                    """
                        import ui.Command;
                        import ui.commands.ExampleCommand;

                        public class Main {
                            public static void main(String[] args) {
                                Command command = new ExampleCommand();
                                command.execute();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testOnlyPublicFields() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public final String a = ""; //# not ok
                            String b = ""; //# not ok
                            public static final String C = ""; //# ok
                            public String d = ""; //# not ok
                            
                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertDifferentVisibilityField(problems.next(), "a", "private");
        assertDifferentVisibilityField(problems.next(), "b", "private");
        assertDifferentVisibilityField(problems.next(), "d", "private");

        problems.assertExhausted();
    }

    @Test
    void testEnum() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "model.Fruit",
                    """
                        package model;

                        public enum Fruit {
                            STRAWBERRY;

                            public static String getString() {
                                return STRAWBERRY.toString();
                            }
                        }
                        """
                ),
                Map.entry(
                    "Main",
                    """
                        import model.Fruit;

                        public class Main {
                            public static void main(String[] args) {
                                System.out.println(Fruit.getString());
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConventionExceptionConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                throw new MyException("abc123", 123);
                            }
                        }
                        """
                ),
                Map.entry(
                    "MyException",
                    """
                        public class MyException extends RuntimeException {
                            public MyException() {}
                            
                            public MyException(String message) { super(message); }
                            public MyException(String message, Throwable cause) { super(message, cause); }
                            public MyException(Throwable cause) { super(cause); }
                            
                            public MyException(String message, int number) { super(message + number); } // this is used
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConstructorImplicitSuper() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                Parent parent = new Child();
                                System.out.println(parent);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Parent",
                    """
                    public abstract class Parent {
                        protected Parent() {
                        }
                    }
                    """
                ),
                Map.entry(
                    "Child",
                    """
                    public class Child extends Parent {
                        public Child() {
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConstructorImplicitSuperTwice() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                Parent parent = new SubChild();
                                System.out.println(parent);
                            }
                        }
                        """
                ),
                Map.entry(
                    "Parent",
                    """
                    public abstract class Parent {
                        protected Parent() {
                        }
                    }
                    """
                ),
                Map.entry(
                    "Child",
                    """
                    public class Child extends Parent {
                    }
                    """
                ),
                Map.entry(
                    "SubChild",
                    """
                    public class SubChild extends Child {
                        public SubChild() {
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testOverrideEnum() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                Command.call();
                            }
                        }
                        """
                ),
                Map.entry(
                    "Command",
                    """
                    enum Command {
                        START_SESSION {
                            @Override
                            protected void execute() {}
                        },
                        END_SESSION {
                            @Override
                            protected void execute() {}
                        };

                        protected void execute() {}

                        static void call() {
                            for (var value : Command.values()) {
                                value.execute();
                            }
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertDifferentVisibility(problems.next(), "execute", "default");

        problems.assertExhausted();
    }


    @Test
    void testEnumWithPrivateInterface() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                Command.call();
                            }
                        }
                        """
                ),
                Map.entry(
                    "Command",
                    """
                    enum Command {
                        START_SESSION(() -> { System.out.println("Started Session"); }),
                        END_SESSION(() -> { System.out.println("Ended Session"); });

                        private final CommandInterface commandInterface;
                        
                        Command(CommandInterface commandInterface) {
                            this.commandInterface = commandInterface;
                        }

                        static void call() {
                            for (var value : Command.values()) {
                                value.commandInterface.execute();
                            }
                        }
                        
                        @FunctionalInterface
                        private interface CommandInterface {
                            void execute();
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testEnumWithPrivateInterfaceIndirectCall() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            public static void main(String[] args) {
                                Command.call();
                            }
                        }
                        """
                ),
                Map.entry(
                    "Command",
                    """
                    enum Command {
                        START_SESSION(Command::call),
                        END_SESSION(Command::call);

                        private final CommandInterface commandInterface;
                        
                        Command(CommandInterface commandInterface) {
                            this.commandInterface = commandInterface;
                        }

                        static void call() {
                            for (var value : Command.values()) {
                                value.commandInterface.execute();
                            }
                        }
                        
                        @FunctionalInterface
                        private interface CommandInterface {
                            void execute();
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
