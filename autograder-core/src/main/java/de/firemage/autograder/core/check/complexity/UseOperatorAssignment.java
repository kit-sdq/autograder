package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.TypeUtil;
import de.firemage.autograder.core.integrated.evaluator.OperatorHelper;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.declaration.CtTypedElement;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ExecutableCheck(reportedProblems = {ProblemType.USE_OPERATOR_ASSIGNMENT})
public class UseOperatorAssignment extends IntegratedCheck {
    private static final List<Class<?>> NON_COMMUTATIVE_TYPES = List.of(
        java.lang.String.class
    );
    private static final Set<BinaryOperatorKind> NON_ASSIGNABLE_OPERATORS = Set.of(
        BinaryOperatorKind.AND,
        BinaryOperatorKind.EQ,
        BinaryOperatorKind.GE,
        BinaryOperatorKind.GT,
        BinaryOperatorKind.INSTANCEOF,
        BinaryOperatorKind.LE,
        BinaryOperatorKind.LT,
        BinaryOperatorKind.NE,
        BinaryOperatorKind.OR
    );

    private boolean isCommutativeType(CtTypedElement<?> ctTypedElement) {
        return ctTypedElement.getType() == null
               || NON_COMMUTATIVE_TYPES.stream()
                                       .noneMatch(ty -> TypeUtil.isTypeEqualTo(ctTypedElement.getType(), ty));
    }

    private boolean isCommutative(BinaryOperatorKind binaryOperatorKind) {
        return List.of(
            BinaryOperatorKind.AND,
            BinaryOperatorKind.OR,
            BinaryOperatorKind.BITXOR,
            BinaryOperatorKind.MUL,
            BinaryOperatorKind.PLUS
        ).contains(binaryOperatorKind);
    }

    private boolean isAssignable(BinaryOperatorKind binaryOperatorKind) {
        return !NON_ASSIGNABLE_OPERATORS.contains(binaryOperatorKind) && (this.isCommutative(binaryOperatorKind) || List.of(
            BinaryOperatorKind.MOD,
            BinaryOperatorKind.MINUS,
            BinaryOperatorKind.DIV,
            BinaryOperatorKind.SL,
            BinaryOperatorKind.SR
        ).contains(binaryOperatorKind));
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtAssignment<?, ?>>() {
            @Override
            public void process(CtAssignment<?, ?> assignment) {
                // skip operator assignments:
                if (assignment instanceof CtOperatorAssignment<?, ?>) {
                    return;
                }

                CtExpression<?> lhs = assignment.getAssigned();
                CtExpression<?> rhs = assignment.getAssignment();
                if (lhs == null || rhs == null) {
                    return;
                }

                if (!(rhs instanceof CtBinaryOperator<?> ctBinaryOperator)) {
                    return;
                }

                BinaryOperatorKind operator = ctBinaryOperator.getKind();
                if (!isAssignable(operator)) {
                    return;
                }

                CtExpression<?> left = ctBinaryOperator.getLeftHandOperand();
                CtExpression<?> right = ctBinaryOperator.getRightHandOperand();

                String simplifiedExpr = null;
                if (left.toString().equals(lhs.toString())) {
                    // left hand side is the same, so we can use an operator assignment
                    simplifiedExpr = "%s %s= %s".formatted(lhs, OperatorHelper.getOperatorText(operator), right);
                } else if (isCommutative(operator) && isCommutativeType(ctBinaryOperator) && right.toString().equals(lhs.toString())) {
                    // operator is commutative so <lhs> = <left> <op> <right> is equivalent to
                    // <lhs> = <right> <op> <left>

                    simplifiedExpr = "%s %s= %s".formatted(lhs, OperatorHelper.getOperatorText(operator), left);
                } else if (
                    // The expression str1 = str1 + str2 + str3 is given as str1 = (str1 + str2) + str3
                    TypeUtil.isTypeEqualTo(lhs.getType(), String.class)
                        && operator == BinaryOperatorKind.PLUS) {

                }

                if (simplifiedExpr != null) {
                    addLocalProblem(
                        assignment,
                        new LocalizedMessage(
                            "common-reimplementation",
                            Map.of("suggestion", simplifiedExpr)
                        ),
                        ProblemType.USE_OPERATOR_ASSIGNMENT
                    );
                }
            }
        });
    }
}
