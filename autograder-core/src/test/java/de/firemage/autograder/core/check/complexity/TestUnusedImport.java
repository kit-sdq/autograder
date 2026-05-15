package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUnusedImport extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.UNUSED_IMPORT);

    void assertUnusedImport(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "unused-import",
                Map.of("import", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testDuplicateImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;
            import java.util.List;

            class Test {}
            """
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import java.util.List;");

        problems.assertExhausted();
    }

    @Test
    void testSingleUnusedImportType() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.io.File;

            class Test {}
            """
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import java.io.File;");

        problems.assertExhausted();
    }

    @Test
    void testSingleUsedImportType() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.io.File;

            class Test {
                private File file;
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultipleUnusedImportType() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.io.File;
            import java.util.List;

            class Test {
            }
            """
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import java.util.List;");
        assertUnusedImport(problems.next(), "import java.io.File;");

        problems.assertExhausted();
    }

    @Test
    void testStaticTypeAccess() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;

            class Test {
                void foo() {
                    List.of();
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testOnlyUsedInThrows() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.io.IOException;

            class Test {
                void foo() throws IOException {}
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedInGenerics() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import foo.MyInterface;

                import java.util.Collection;
                import java.util.List;
                import java.util.ArrayList;

                class Test {
                    List<MyInterface> list;
                    List<Collection> x = new ArrayList<Collection>();
                }
                """,
                "foo.MyInterface",
                """
                package foo;
                
                public interface MyInterface {}
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedStaticImportField() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static java.lang.Math.PI;

                class Test {
                    double x = PI;
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedStaticImportMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static java.lang.Math.pow;

                class Test {
                    double x = pow(2, 3);
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUnusedStaticImportField() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static java.lang.Math.PI;

                class Test {}
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import static java.lang.Math.PI;");

        problems.assertExhausted();
    }

    @Test
    void testUnusedStaticImportMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static java.lang.Math.pow;

                class Test {}
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import static java.lang.Math.pow;");

        problems.assertExhausted();
    }

    @Test
    void testPartiallyUsedTypeImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import foo.A;
                import foo.B;

                class Test {
                    A a;
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {}
                """,
                "foo.B",
                """
                package foo;
                
                public class B {}
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import foo.B;");

        problems.assertExhausted();
    }

    @Test
    void testUnusedTypeImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import foo.A;
                import foo.B;

                class Test {
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {}
                """,
                "foo.B",
                """
                package foo;
                
                public class B {}
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import foo.A;");
        assertUnusedImport(problems.next(), "import foo.B;");

        problems.assertExhausted();
    }

    @Test
    void testConstantsUsed() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static foo.A.X;
                import static foo.A.Y;

                class Test {
                    int x = X;
                    int y = Y;
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {
                    public static final int X = 0;
                    public static final int Y = 0;
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testImportedConstantUnused() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static foo.A.X;

                class Test {
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {
                    public static final int X = 0;
                    public static final int Y = 0;
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import static foo.A.X;");

        problems.assertExhausted();
    }

    @Test
    void testConstantUsedWithInvocation() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static foo.A.X;

                class Test {
                    public static void main(String[] args) {
                        if (X.equals("a")) {
                            System.out.println("X is a");
                        }
                    }
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {
                    public static final String X = "0";
                    public static final int Y = 0;
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedPackagePrivateMethod() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "foo.Test",
                """
                package foo;

                import static foo.A.doSomething;

                class Test {
                    public static void main(String[] args) {
                        doSomething();
                    }
                }
                """,
                "foo.A",
                """
                package foo;
                
                final class A {
                    private A() {}
                    
                    static void doSomething() {}
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedExtends() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.util.ArrayList;

                class Test<T> extends ArrayList<T> {
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedImplements() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.util.List;

                abstract class Test<T> implements List<T> {
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedArrayType() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.util.List;

                class Test {
                    List<String>[] array;
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedArrayInit() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.util.ArrayList;
                import java.util.List;

                class Test {
                    List<String>[] array = new ArrayList[0];
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    @Disabled("Too much work to implement for an edge case")
    void testUnusedWithMemberAndInherited() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "foo.Test",
                """
                package foo;

                import static foo.A.doSomething;

                class Test extends A {
                    void foo() {
                        // unused, because method is inherited
                        doSomething();
                    }
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {
                    static void doSomething() {}
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import static foo.A.doSomething;");

        problems.assertExhausted();
    }

    @Test
    void testImportSamePackage() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "foo.Test",
                """
                package foo;

                import foo.A;

                class Test extends A {
                }
                """,
                "foo.A",
                """
                package foo;
                
                public class A {
                    void doSomething() {}
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import foo.A;");

        problems.assertExhausted();
    }

    @Test
    void testUsedJavaLangImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.lang.String;

                class Test {
                    String string;
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import java.lang.String;");

        problems.assertExhausted();
    }

    @Test
    void testUnusedJavaLangImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.lang.String;

                class Test {
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import java.lang.String;");

        problems.assertExhausted();
    }

    @Test
    void testUnusedStaticJavaLangImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static java.lang.String.valueOf;

                class Test {
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import static java.lang.String.valueOf;");

        problems.assertExhausted();
    }

    @Test
    @Disabled("Some spoon bug that I can't be bothered to report, debug or fix. Who writes code like this anyway?")
    void testUsedStaticJavaLangImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import static java.lang.String.valueOf;

                class Test {
                    void foo() {
                        valueOf(1);
                    }
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUnusedJavaLangNestedClassImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.lang.Thread.UncaughtExceptionHandler;

                class Test {
                    Thread.UncaughtExceptionHandler handler;
                }
                """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import java.lang.Thread.UncaughtExceptionHandler;");

        problems.assertExhausted();
    }

    @Test
    void testUsedJavaLangNestedClassImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.lang.Thread.UncaughtExceptionHandler;

                class Test {
                    UncaughtExceptionHandler handler;
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedSamePackageNestedClassImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "foo.Test",
                """
                package foo;
                
                import foo.A.B;

                class Test {
                    B b;
                }
                """,
                "foo.A",
                """
                    package foo;
                    
                    public class A {
                        public static class B {}
                    }
                    """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUnusedSamePackageNestedClassImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "foo.Test",
                """
                package foo;
                
                import foo.A.B;

                class Test {
                }
                """,
                "foo.A",
                """
                    package foo;
                    
                    public class A {
                        public static class B {}
                    }
                    """
            )
        ), PROBLEM_TYPES);

        assertUnusedImport(problems.next(), "import foo.A.B;");

        problems.assertExhausted();
    }

    @Test
    void testUsedJavaLangSubpackageImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.lang.invoke.MethodHandles;

                class Test {
                    MethodHandles handler;
                }
                """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUnusedJavaLangSubpackageImport() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.lang.invoke.MethodHandles;

                class Test {
                }
                """
            )
        ), PROBLEM_TYPES);
        assertUnusedImport(problems.next(), "import java.lang.invoke.MethodHandles;");

        problems.assertExhausted();
    }

    @Test
    void testUsedJavadoc(@TempDir Path tempDir) throws IOException, LinterException {
        // Javadoc Parsing requires the file to be on disk
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "Test",
                """
                import java.util.ArrayList;
                import java.util.Calendar;
                import java.util.List;
                import java.util.LinkedList;
                import java.io.File;
                import java.util.NoSuchElementException;
                import java.io.IOException;

                class Test {
                    /**
                     * {@linkplain  List list}
                     * {@link  ArrayList  arraylist}
                     * {@link  LinkedList}
                     * {@value Calendar}
                     * @see File
                     * @throws NoSuchElementException no such element
                     * @exception IOException IO operation exception
                     */
                    void foo() {}
                }
                """
            )
        ).copyTo(tempDir), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


