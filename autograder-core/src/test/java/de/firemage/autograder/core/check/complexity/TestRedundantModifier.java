package de.firemage.autograder.core.check.complexity;

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

class TestRedundantModifier extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.REDUNDANT_MODIFIER);

    void assertEqualsRedundant(Problem problem, String... modifiers) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "redundant-modifier",
                Map.of("modifier", String.join(", ", modifiers))
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testInterfaceFieldsMethods() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public interface Test {
                    public int fieldA = 0; //# not ok: public
                    static int fieldB = 0; //# not ok: static
                    final int fieldC = 0; //# not ok: final

                    public static int fieldD = 0; //# not ok: public, static
                    public static final int fieldE = 0; //# not ok: public, static, final

                    int fieldF = 0; //# ok
                    static final int fieldG = 0; //# not ok: static, final


                    public abstract void methodA(); //# not ok: public, abstract

                    public void methodB(); //# not ok: public

                    public static void methodC() {} //# not ok: public

                    private static void methodD() {} //# ok
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "public");
        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "final");

        assertEqualsRedundant(problems.next(), "public", "static");
        assertEqualsRedundant(problems.next(), "public", "static", "final");
        assertEqualsRedundant(problems.next(), "static", "final");


        assertEqualsRedundant(problems.next(), "public", "abstract");
        assertEqualsRedundant(problems.next(), "public");
        assertEqualsRedundant(problems.next(), "public");

        problems.assertExhausted();
    }

    @Test
    void testInterfaceTypeMembers() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public interface Test {
                    class NestedClass {} //# ok
                    interface NestedInterface {} //# ok
                    enum NestedEnum {} //# ok
                    record NestedRecord() {} //# ok
                    
                    public class NestedClass2 {} //# not ok: public
                    public interface NestedInterface2 {} //# not ok: public
                    public enum NestedEnum2 {} //# not ok: public
                    public record NestedRecord2() {} //# not ok: public
                    
                    static class NestedClass3 {} //# not ok: static
                    static interface NestedInterface3 {} //# not ok: static
                    static enum NestedEnum3 {} //# not ok: static
                    static record NestedRecord3() {} //# not ok: static
                    
                    public static class NestedClass4 {} //# not ok: public, static
                    public static interface NestedInterface4 {} //# not ok: public, static
                    public static enum NestedEnum4 {} //# not ok: public, static
                    public static record NestedRecord4() {} //# not ok: public, static
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "public");
        assertEqualsRedundant(problems.next(), "public");
        assertEqualsRedundant(problems.next(), "public");
        assertEqualsRedundant(problems.next(), "public");

        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "static");

        assertEqualsRedundant(problems.next(), "public", "static");
        assertEqualsRedundant(problems.next(), "public", "static");
        assertEqualsRedundant(problems.next(), "public", "static");
        assertEqualsRedundant(problems.next(), "public", "static");

        problems.assertExhausted();
    }

    @Test
    void testImplicitlyFinalRecord() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            "public final record Test() {}"
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "final");

        problems.assertExhausted();
    }

    /**
     * When an enum variant overrides a method, the method could have a final modifier.
     * <p>
     * The modifier is redundant in the override, because one can not extend an enum variant.
     */
    @Test
    void testEnumMethodOverrideExtraFinal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public enum Field {
                    STONE,
                    MOUNTAIN {
                        @Override
                        public final String getName() { //# not ok: final is redundant
                            return "mountain";
                        }
                    };

                    public String getName() {
                        return this.toString();
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsRedundant(problems.next(), "final");

        problems.assertExhausted();
    }

    /**
     * An enum with fields that do not have an override block, is declared final.
     * <p>
     * Therefore the final modifier is redundant for the methods.
     */
    @Test
    void testFinalEnumMethodNoOverride() throws IOException, LinterException {
        // NOTE: No field has a {} block, therefore the enum is final and the final modifier is redundant for
        //       the method
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public enum Field {
                    STONE,
                    MOUNTAIN;

                    public final void print() { //# not ok
                        System.out.println(this.toString());
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "final");

        problems.assertExhausted();
    }

    /**
     * An enum with fields that have an override block could override a method,
     * therefore a final modifier is not redundant.
     */
    @Test
    void testFinalEnumMethodOverride() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Field",
            """
                public enum Field {
                    STONE {},
                    MOUNTAIN;

                    public final void print() { //# ok
                        System.out.println(this.toString());
                    }
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testEnumTypeMembers() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public enum Test {
                    VALUE;
                    
                    class NestedClass {} //# ok
                    interface NestedInterface {} //# ok
                    enum NestedEnum {} //# ok
                    record NestedRecord() {} //# ok

                    static class NestedClass2 {} //# not ok: static
                    static interface NestedInterface2 {} //# not ok: static
                    static enum NestedEnum2 {} //# not ok: static
                    static record NestedRecord2() {} //# not ok: static
                }
                """
        ), PROBLEM_TYPES);

        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "static");
        assertEqualsRedundant(problems.next(), "static");

        problems.assertExhausted();
    }

    @Test
    void testEnumConstructorModifier() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public enum Test {
                    A();
                    
                    private Test() {} //# not ok: private
                }
                """
        ), List.of(ProblemType.REDUNDANT_MODIFIER, ProblemType.REDUNDANT_MODIFIER_VISIBILITY_ENUM_CONSTRUCTOR));

        assertEqualsRedundant(problems.next(), "private");

        problems.assertExhausted();
    }
}


