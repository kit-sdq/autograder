package de.firemage.autograder.extra.check.naming;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLinguisticNamingCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.CONFUSING_IDENTIFIER);

    void assertEqualsBoolean(String name, String type, Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "linguistic-naming-boolean",
                Map.of("name", name, "type", type)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsGetter(String name, Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "linguistic-naming-getter",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    void assertEqualsSetter(String name, Problem problem) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "linguistic-naming-setter",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSetter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    // ok
                    public void setSomething(String string) {
                    }
                    
                    // not ok
                    public int setString(String string) {
                        return 1;
                    }
                    
                    // ignored, because of wrong naming convention
                    public int setignored(String string) {
                        return 1;
                    }
                    
                    // ok, returns old value
                    public String setValue(String string) {
                        return string;
                    }
                    
                    // ok, useful for chaining
                    public Test setDouble(double value) {
                        return this;
                    }
                    
                    // not ok
                    public static Test setStatic(String string) {
                        return new Test();
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsSetter("setString", problems.next());
        assertEqualsSetter("setStatic", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testGetter() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    // not ok
                    public void getSomething() {
                    }

                    // ok
                    public String getString() {
                        return "";
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsGetter("getSomething", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testBoolean() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final String HAS_VALUE = ""; // not ok
                    private static final boolean ARE_VALID = true; // ok

                    private final String value = ""; // ok
                    private final Integer hasNumber = 1; // not ok
                    private final boolean hasValidNumber = false; // ok

                    // not ok
                    public void areValid() {
                    }
                    
                    // ok
                    public boolean isValidValue() {
                        return false;
                    }

                    // not ok
                    public String isInvalid(String value) {
                        return value;
                    }
                    
                    // ok
                    public boolean isInvalidValue(String value) {
                        return !value.isEmpty();
                    }
                }
                """
        ), PROBLEM_TYPES);


        assertEqualsBoolean("HAS_VALUE", "String", problems.next());
        assertEqualsBoolean("hasNumber", "Integer", problems.next());
        assertEqualsBoolean("areValid", "void", problems.next());
        assertEqualsBoolean("isInvalid", "String", problems.next());

        problems.assertExhausted();
    }

    @Test
    void testVariablesToIgnore() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                public class Test {
                    private static final String HAS_VALUE_REGEX = "value"; // ok
                    private static final String HAS_VALUE_PATTERN = "value"; // ok
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testEnumValue() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "ResultState",
            """
                public enum ResultState {
                    SUCCESS,
                    FAILURE,
                    MUST_MOVE_OBSTACLE,
                    WINNER;
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
