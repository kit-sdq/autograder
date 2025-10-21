package de.firemage.autograder.core.integrated.evaluator.algebra;

import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.FactoryUtil;
import de.firemage.autograder.core.integrated.evaluator.fold.Fold;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In Java when you have an expression like `a || b`, `b` will only be evaluated if `a` is false.
 * Similarly, in `a && b`, `b` will only be evaluated if `a` is true.
 * <p>
 * This fold applies this short-circuiting behavior when evaluating expressions, replacing parts
 * that would evaluate to `true` or `false` with their literal value.
 */
public final class ApplyShortCircuit implements Fold {
    private final List<OperatorContext> contexts;

    private ApplyShortCircuit() {
        this.contexts = new ArrayList<>();
    }

    public static Fold create() {
        return new ApplyShortCircuit();
    }

    private record OperatorContext(CtExpression<?> expression, boolean value) {
        public Optional<CtLiteral<Boolean>> evaluate(CtExpression<?> ctExpression) {
            if (!ExpressionUtil.isBoolean(ctExpression)) {
                return Optional.empty();
            }

            var booleanType = this.expression.getFactory().Type().booleanPrimitiveType();

            if (AlgebraUtils.isEqualTo(this.expression, ctExpression)) {
                return Optional.of(FactoryUtil.makeLiteral(booleanType, this.value));
            }

            if (AlgebraUtils.isNegatedEqualTo(this.expression, ctExpression)) {
                return Optional.of(FactoryUtil.makeLiteral(booleanType, !this.value));
            }

            return Optional.empty();
        }
    }

    private static OperatorContext getContext(CtExpression<?> ctExpression) {
        // Check if the expression is the right-hand side of a binary operator
        if (ctExpression.isParentInitialized() && ctExpression.getParent() instanceof CtBinaryOperator<?> parentBinaryOperator
            && parentBinaryOperator.getRightHandOperand() == ctExpression) {
            // in that case, it is guaranteed that the left-hand side has already been evaluated

            // It's evaluation depends on the operator kind
            switch (parentBinaryOperator.getKind()) {
                case OR -> {
                    // In a || b the a evaluates to false while b is evaluated, because if a were true, b would not be evaluated
                    return new OperatorContext(parentBinaryOperator.getLeftHandOperand().clone(), false);
                }
                case AND -> {
                    return new OperatorContext(parentBinaryOperator.getLeftHandOperand().clone(), true);
                }
            }
        }

        return null;
    }

    @Override
    public CtElement enter(CtElement ctElement) {
        if (ctElement instanceof CtExpression<?> ctExpression) {
            var context = getContext(ctExpression);
            if (context != null) {
                this.contexts.add(context);
            }
        }

        return ctElement;
    }

    @Override
    public CtElement exit(CtElement ctElement) {
        var result = this.fold(ctElement);

        // After folding, we need to remove the context if it was added, because it is only valid in the context
        if (ctElement instanceof CtExpression<?> ctExpression) {
            if (getContext(ctExpression) != null) {
                this.contexts.removeLast();
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CtExpression<T> foldCtExpression(CtExpression<T> ctExpression) {
        for (OperatorContext context : this.contexts) {
            var result = context.evaluate(ctExpression).orElse(null);
            if (result != null) {
                return (CtExpression<T>) result;
            }
        }

        return ctExpression;
    }
}
