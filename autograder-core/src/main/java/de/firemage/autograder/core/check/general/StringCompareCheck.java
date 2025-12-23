package de.firemage.autograder.core.check.general;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.TypeUtil;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;

import java.util.Map;

@ExecutableCheck(reportedProblems = { ProblemType.STRING_COMPARE_BY_REFERENCE })
public class StringCompareCheck extends IntegratedCheck {
    private static boolean isStringComparison(CtExpression<?> lhs, CtExpression<?> rhs) {
        return TypeUtil.isString(lhs.getType()) && !ExpressionUtil.isNullLiteral(rhs)
               || TypeUtil.isString(rhs.getType()) && !ExpressionUtil.isNullLiteral(lhs);
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtBinaryOperator<?>>() {
            @Override
            public void process(CtBinaryOperator<?> operator) {
                if (operator.getKind() != BinaryOperatorKind.EQ && operator.getKind() != BinaryOperatorKind.NE) {
                    return;
                }

                CtExpression<?> lhs = operator.getLeftHandOperand();
                CtExpression<?> rhs = operator.getRightHandOperand();

                if (isStringComparison(lhs, rhs)) {
                    addLocalProblem(operator, new LocalizedMessage(
                            "suggest-replacement",
                            Map.of(
                                "original", operator.toString(),
                                "suggestion", "%s.equals(%s)".formatted(lhs, rhs)
                            )
                        ),
                        ProblemType.STRING_COMPARE_BY_REFERENCE
                    );
                }
            }
        });
    }
}
