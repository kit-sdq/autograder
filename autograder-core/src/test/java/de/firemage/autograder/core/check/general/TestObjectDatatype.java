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

class TestObjectDatatype extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.OBJECT_DATATYPE);

    void assertHasObject(Problem problem, String variable) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "object-datatype",
                Map.of("variable", variable)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testObjectVariable() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                Object field;

                void method() {
                    Object local = "abc";
                }
            }
            """
        ), PROBLEM_TYPES);

        assertHasObject(problems.next(), "field");
        assertHasObject(problems.next(), "local");
        problems.assertExhausted();
    }

    @Test
    void testObjectVariableArray() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                Object[] field1;
                Object[][] field2;

                void method() {
                    Object[][] local = null;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testObjectEquals() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            class Test {
                @Override
                public boolean equals(Object o) {
                    return false;
                }
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testGenerics() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;

            class Test<T> {
                List<T> list;
                List<?> erased;
                T field;
                
                List<Object> objects;
            }
            """
        ), PROBLEM_TYPES);

        assertHasObject(problems.next(), "objects");

        problems.assertExhausted();
    }

    @Test
    void testRawTypes() throws IOException, LinterException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
            import java.util.List;

            class Test {
                List list;
            }
            """
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}


