package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.FactoryUtil;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.TypeUtil;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.declaration.CtTypedElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExecutableCheck(reportedProblems = {ProblemType.USE_OPERATOR_ASSIGNMENT})
public class UseOperatorAssignment extends IntegratedCheck {
    private static final List<Class<?>> NON_COMMUTATIVE_TYPES = List.of(String.class);
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

    private static boolean isCommutativeType(CtTypedElement<?> ctTypedElement) {
        return ctTypedElement.getType() == null
               || NON_COMMUTATIVE_TYPES.stream()
                                       .noneMatch(ty -> TypeUtil.isTypeEqualTo(ctTypedElement.getType(), ty));
    }

    private static boolean isCommutative(BinaryOperatorKind binaryOperatorKind) {
        return List.of(
            BinaryOperatorKind.AND,
            BinaryOperatorKind.OR,
            BinaryOperatorKind.BITXOR,
            BinaryOperatorKind.MUL,
            BinaryOperatorKind.PLUS
        ).contains(binaryOperatorKind);
    }

    private static boolean isAssignable(BinaryOperatorKind binaryOperatorKind) {
        return !NON_ASSIGNABLE_OPERATORS.contains(binaryOperatorKind) && (isCommutative(binaryOperatorKind) || List.of(
            BinaryOperatorKind.MOD,
            BinaryOperatorKind.MINUS,
            BinaryOperatorKind.DIV,
            BinaryOperatorKind.SL,
            BinaryOperatorKind.SR
        ).contains(binaryOperatorKind));
    }

    /**
     * Splits a {@link CtBinaryOperator} at the given {@link BinaryOperatorKind}. This is useful for expressions like
     * `a + b + c + d` which would be split into `a`, `b`, `c`, and `d` when splitting at `BinaryOperatorKind.PLUS`.
     *
     * @param ctBinaryOperator the binary operator to split
     * @param binaryOperatorKind the kind of operator to split at
     * @return a list of expressions or empty if the operator kind does not match
     */
    private static List<CtExpression<?>> splitCtBinaryOperator(CtBinaryOperator<?> ctBinaryOperator, BinaryOperatorKind binaryOperatorKind) {
        if (ctBinaryOperator.getKind() != binaryOperatorKind) {
            return List.of();
        }

        List<CtExpression<?>> result = new ArrayList<>();

        var leftHandOperand = ctBinaryOperator.getLeftHandOperand();
        var rightHandOperand = ctBinaryOperator.getRightHandOperand();

        if (leftHandOperand instanceof CtBinaryOperator<?> leftBinaryOperator && leftBinaryOperator.getKind() == binaryOperatorKind) {
            result.addAll(splitCtBinaryOperator(leftBinaryOperator, binaryOperatorKind));
        } else {
            result.add(leftHandOperand);
        }

        if (rightHandOperand instanceof CtBinaryOperator<?> rightBinaryOperator && rightBinaryOperator.getKind() == binaryOperatorKind) {
            result.addAll(splitCtBinaryOperator(rightBinaryOperator, binaryOperatorKind));
        } else {
            result.add(rightHandOperand);
        }

        return result;
    }

    private static CtExpression<?> joinCtBinaryOperator(
        List<? extends CtExpression<?>> parts,
        BinaryOperatorKind binaryOperatorKind
    ) {
        if (parts.isEmpty()) {
            throw new IllegalStateException("parts should not be empty");
        }

        if (parts.size() == 1) {
            return parts.getFirst();
        }

        CtExpression<?> left = parts.getFirst();
        for (CtExpression<?> right : parts.subList(1, parts.size())) {
            left = FactoryUtil.createBinaryOperator(left, right, binaryOperatorKind);
        }

        return left;
    }

    private static CtExpression<?> foldCtAssignment(CtAssignment<?, ?> ctAssignment) {
        // skip operator assignments:
        if (ctAssignment instanceof CtOperatorAssignment<?, ?>) {
            return ctAssignment;
        }

        // Both sides of the assignment must be present (obviously), and the rhs should be a binary operator
        CtExpression<?> lhs = ctAssignment.getAssigned();
        CtExpression<?> rhs = ctAssignment.getAssignment();
        if (lhs == null || !(rhs instanceof CtBinaryOperator<?> ctBinaryOperator)) {
            return ctAssignment;
        }

        // We can only simplify assignments where the operator is assignable,
        // for example `a = a == b` can not be written as `a === b`
        BinaryOperatorKind operator = ctBinaryOperator.getKind();
        if (!isAssignable(operator)) {
            return ctAssignment;
        }

        // To support for example `a = b + c + a + d`, we split it by the operator
        List<CtExpression<?>> parts = splitCtBinaryOperator(ctBinaryOperator, operator);
        if (parts.isEmpty()) {
            return ctAssignment;
        }

        List<CtExpression<?>> rightSide = new ArrayList<>();
        // This finds the first index where the lhs occurs in the parts (if it does)
        int matchingIndex = -1;
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            // Directly using equals does not work, because things that are supposed to be equal,
            // are not. The .toString() comparison is a temporary solution, until the structural
            // equality code becomes usable for this use-case.
            if (matchingIndex == -1 && lhs.toString().equals(part.toString())) {
                matchingIndex = i;
                continue;
            }

            rightSide.add(part);
        }

        // If the lhs does not occur in the rhs, just return
        if (matchingIndex == -1 || rightSide.isEmpty()) {
            return ctAssignment;
        }

        // If the operator (or type for strings) is not commutative, the matching part must be
        // the first element, if it is not the simplification would be wrong, for example:
        // str1 = str2 + str1 + str3;
        // can not be optimized to
        // str1 += str2 + str3;
        //
        // If the variables were numbers, this would be okay to do.

        if (matchingIndex != 0 && (!isCommutative(operator) || !isCommutativeType(lhs))) {
            return ctAssignment;
        }

        return FactoryUtil.createOperatorAssignment(
            lhs,
            joinCtBinaryOperator(rightSide, operator),
            operator
        );
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtAssignment<?, ?>>() {
            @Override
            public void process(CtAssignment<?, ?> assignment) {
                if (!assignment.getPosition().isValidPosition() || assignment.isImplicit()) {
                    return;
                }

                if (assignment instanceof CtOperatorAssignment<?, ?>) {
                    // already an operator assignment, so skip it
                    return;
                }

                var simplifiedExpr = foldCtAssignment(assignment);

                if (simplifiedExpr instanceof CtOperatorAssignment<?,?>) {
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
