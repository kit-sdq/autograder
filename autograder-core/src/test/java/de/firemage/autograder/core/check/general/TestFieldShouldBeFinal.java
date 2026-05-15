package de.firemage.autograder.core.check.general;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFieldShouldBeFinal extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.FIELD_SHOULD_BE_FINAL);

    void assertFinal(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "field-should-be-final",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMultipleAssignments() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    String value;

                    Test() {
                        this.value = "Hello World";
                    }

                    void foo() {
                        this.value = "Value";
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAssignedOnlyInConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    String value;

                    Test() {
                        this.value = "Hello World";
                    }

                    @Override
                    public String toString() {
                        return this.value;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertFinal(problems.next(), "value");

        problems.assertExhausted();
    }

    @Test
    void testInitializedAndAssignedInConstructor() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    int value = 1;

                    Test() {
                        this.value = 2;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAlreadyFinal() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                class Test {
                    final int value;

                    Test() {
                        this.value = 2;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testPartialConstructorInit() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "A",
            """
                abstract class A {
                    private String value = null;

                    protected A() {

                    }

                    protected A(String value) {
                        this.value = value;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticVariableInlineAssignedInConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                class User {
                    static int nextId;
                    final int id;

                    User() {
                        this.id = nextId++;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticVariableOnlyInitialized() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                class User {
                    static int nextId = 1;
                    final int id;

                    User() {
                        this.id = nextId;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertFinal(problems.next(), "nextId");

        problems.assertExhausted();
    }

    @Test
    void testStaticVariableAssignedConstantInConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                class User {
                    static int nextId;
                    final int id;

                    User() {
                        nextId = 1;
                        this.id = nextId;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticVariableDynamicAssignment() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                class User {
                    static int nextId;
                    final int id;

                    User(int next) {
                        nextId = next;
                        this.id = next;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testRecord() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public record User(String name, int id) {
                    public User(String name, int id) {
                        this.name = name;
                        this.id = id;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testClassNoConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public abstract class User {
                    protected String name;
                    protected int id;

                    public String getName() {
                        return name;
                    }
                    
                    public int getId() {
                        return id;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testAbstractClassProtectedFields() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.ofEntries(
                Map.entry(
                    "User",
                    """
                    public abstract class User {
                        protected String name;
                        protected int id;

                        protected User(String name, int id) {
                            this.name = name;
                            this.id = id;
                        }

                        public String getName() {
                            return name;
                        }

                        public int getId() {
                            return id;
                        }

                        public static void main(String[] args) {}
                    }
                    """
                ),
                Map.entry(
                    "Admin",
                    """
                    public class Admin extends User {
                        public Admin() {
                            super("admin", 1);
                            //this.name = "admin";
                            //this.id = 1;
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testPartialInitMultipleConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private String name;
                    private int id;

                    public User(String name) {
                        this.name = name;
                        // no id init -> id = 0
                    }

                    public User(String name, int id) {
                        this.id = id;
                        this.name = name;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertFinal(problems.next(), "name");

        problems.assertExhausted();
    }

    @Test
    void testPartialInitMultipleConstructor2() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private String name;
                    private int id;

                    public User(String name) {
                        this.name = name;
                        // no id init -> id = 0
                    }
                    
                    public User(int id) {
                        this.id = id;
                        // no name init -> name = null
                    }
                    
                    public User() {
                        // no name and id init -> name = null, id = 0
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultipleWriteInConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private int id;

                    public User(String name) {
                        this.name = name;
                        this.id = 0;
                        
                        if (this.name == "admin") {
                            this.id = 1;
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    @Disabled("There is not much value in implementing this")
    void testConditionalWriteInConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private int id;

                    public User(String name) {
                        this.name = name;

                        if (this.name == "admin") {
                            this.id = 1;
                        } else {
                            this.id = 0;
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertFinal(problems.next(), "id");

        problems.assertExhausted();
    }

    @Test
    void testConditionalWriteMultipleInConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private int id;

                    public User(String name) {
                        this.name = name;

                        if (this.name == "admin") {
                            this.id = 1;
                        } else {
                            this.id = 0;
                        }
                        
                        this.id = 5;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testConditionalWriteNoElseConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private int id;

                    public User(String name) {
                        this.name = name;

                        if (this.name == "admin") {
                            this.id = 1;
                        }

                        this.id = 0;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }


    @Test
    void testConditionalWritePartialInit() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private int id;

                    public User(String name) {
                        this.name = name;

                        if (this.name == "admin") {
                            this.id = 1;
                        }
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticFieldOnlyWriteInMethod() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private final int id;
                    private static int next;

                    public User(String name) {
                        this.name = name;
                        this.id = 0;
                    }

                    public void update() {
                        next += 1;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testStaticFieldNoWrite() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private final String name;
                    private final int id;
                    private static int next;

                    public User(String name) {
                        this.name = name;
                        this.id = 0;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testNoConstructorButValue() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private int id = 1;

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertFinal(problems.next(), "id");

        problems.assertExhausted();
    }


    @Test
    void testRecordImplicitConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public record User(int id, String name) {
                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    @Disabled("Some spoon bug in 11.1.1-SNAPSHOT")
    void testRecordStaticField() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public record User(int id, String name) {
                    private static String ADMIN_NAME = "admin";

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        assertFinal(problems.next(), "ADMIN_NAME");

        problems.assertExhausted();
    }

    @Test
    void testInitExtraMethod() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "User",
            """
                public class User {
                    private String name;

                    public User() {
                        setUser("admin");
                    }
                    
                    private void setUser(String name) {
                        this.name = name;
                    }

                    public static void main(String[] args) {}
                }
                """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


