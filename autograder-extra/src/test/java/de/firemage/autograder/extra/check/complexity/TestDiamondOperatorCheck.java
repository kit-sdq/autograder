package de.firemage.autograder.extra.check.complexity;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDiamondOperatorCheck extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(
        ProblemType.UNUSED_DIAMOND_OPERATOR
    );

    void assertRedundantDiamondOperator(Problem problem, String typeArgument) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "use-diamond-operator",
                Map.of("type", typeArgument)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testLocalVariableAssignment() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Example",
                    """
                        import java.util.ArrayList;
                        import java.util.List;

                        public class Example {
                            public static void main(String[] args) {
                                List<String> list = new ArrayList<String>();
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertRedundantDiamondOperator(problems.next(), "<String>");

        problems.assertExhausted();
    }

    @Test
    void testMethodReturn() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "ReproduceIssueInReturn",
                    """
                        public class ReproduceIssueInReturn {
                            static class Pair<L, U> {
                                public Pair(L l, U u) {}
                            }
                        
                            enum VegetableType {
                                TOMATO,
                                POTATO,
                                CARROT
                            }
                        
                        
                            Pair<VegetableType, Integer> makePair(VegetableType vegetable, int price) {
                                return new Pair<VegetableType, Integer>(vegetable, price); /*# not ok #*/
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertRedundantDiamondOperator(problems.next(), "<VegetableType, Integer>");

        problems.assertExhausted();
    }

    @Test
    void testSuperCall() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "ReproduceSuperCallIssue",
                    """
                        import java.util.ArrayList;
                        import java.util.List;

                        public class ReproduceSuperCallIssue {
                            enum A {
                                B, C;
                            }
                        
                            class C {
                                C(List<A> list) {
                                    // ...
                                }
                            }
                        
                            class F extends C {
                                F() {
                                    super(new ArrayList<A>(List.of(A.B, A.C))); /*# not ok #*/
                                }
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertRedundantDiamondOperator(problems.next(), "<A>");

        problems.assertExhausted();
    }
}
