package de.firemage.autograder.core.check.api;
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

class TestUseArraysCopyOf extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_ARRAYS_COPY_OF);

    private void assertUseCopyOf(Problem problem, String destination, String source, String length) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of("suggestion", "%s = Arrays.copyOf(%s, %s)".formatted(destination, source, length))
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testArraysCopyOf() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Arrays;
                
                public class Test {
                    public static void main(String[] args) {
                        String[] destination = new String[10];
                        System.arraycopy(args, 0, destination, 0, args.length);
                        System.out.println(Arrays.toString(destination));
                        
                        System.arraycopy(args, 1, destination, 0, 3);
                        System.out.println(Arrays.toString(destination));

                        System.arraycopy(args, 0, destination, 0, 4);
                        System.out.println(Arrays.toString(destination));
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUseCopyOf(problems.next(), "destination", "args", "args.length");
        assertUseCopyOf(problems.next(), "destination", "args", "4");

        problems.assertExhausted();
    }
}


