package de.firemage.autograder.core.integrated.evaluator.fold;

import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.evaluator.OperatorHelper;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.reference.CtTypeReference;

/**
 * For some binary and unary operator expressions, the operands are cast to a different type before the operation
 * is applied. This fold applies that cast to the operands (if they are literals).
 */
public final class ApplyOperatorPromotion implements Fold {
    public static final OperatorPredicate<? super CtExpression<?>> APPLY_ON_LITERAL_OPERAND =
        (operator, ctExpression) -> ctExpression instanceof CtLiteral<?>;

    private final OperatorPredicate<? super CtBinaryOperator<?>> shouldApplyOnBinaryOperator;
    private final OperatorPredicate<? super CtUnaryOperator<?>> shouldApplyOnUnaryOperator;

    private ApplyOperatorPromotion(
        OperatorPredicate<? super CtBinaryOperator<?>> shouldApplyOnBinaryOperator,
        OperatorPredicate<? super CtUnaryOperator<?>> shouldApplyOnUnaryOperator
    ) {
        this.shouldApplyOnBinaryOperator = shouldApplyOnBinaryOperator;
        this.shouldApplyOnUnaryOperator = shouldApplyOnUnaryOperator;
    }

    /**
     * Creates a new instance of this fold.
     * <p>
     * It applies the operator promotion by default only on binary and unary operators with literal operands.
     *
     * @return the instance of this fold
     */
    public static Fold create() {
        return ApplyOperatorPromotion.create(
            APPLY_ON_LITERAL_OPERAND,
            APPLY_ON_LITERAL_OPERAND
        );
    }

    public static Fold create(
        OperatorPredicate<? super CtBinaryOperator<?>> shouldApplyOnBinaryOperator,
        OperatorPredicate<? super CtUnaryOperator<?>> shouldApplyOnUnaryOperator
    ) {
        return new ApplyOperatorPromotion(shouldApplyOnBinaryOperator, shouldApplyOnUnaryOperator);
    }

    @Override
    public <T> CtExpression<T> foldCtBinaryOperator(CtBinaryOperator<T> ctBinaryOperator) {
        // not implemented for instanceof
        if (ctBinaryOperator.getKind() == BinaryOperatorKind.INSTANCEOF) {
            return ctBinaryOperator;
        }

        CtTypeReference<?> promotedType = OperatorHelper.getPromotedType(
            ctBinaryOperator.getKind(),
            ctBinaryOperator.getLeftHandOperand(),
            ctBinaryOperator.getRightHandOperand()
        ).orElse(null);

        // skip invalid code
        if (promotedType == null) {
            return ctBinaryOperator;
        }

        // only promote if the predicate allows it
        if (this.shouldApplyOnBinaryOperator.shouldApplyOn(ctBinaryOperator, ctBinaryOperator.getLeftHandOperand())) {
            ctBinaryOperator.setLeftHandOperand(ExpressionUtil.castExpression(promotedType, ctBinaryOperator.getLeftHandOperand()));
        }

        if (this.shouldApplyOnBinaryOperator.shouldApplyOn(ctBinaryOperator, ctBinaryOperator.getRightHandOperand())) {
            ctBinaryOperator.setRightHandOperand(ExpressionUtil.castExpression(promotedType, ctBinaryOperator.getRightHandOperand()));
        }

        return ctBinaryOperator;
    }

    @Override
    public <T> CtExpression<T> foldCtUnaryOperator(CtUnaryOperator<T> ctUnaryOperator) {
        CtTypeReference<?> promotedType = OperatorHelper.getPromotedType(
            ctUnaryOperator.getKind(),
            ctUnaryOperator.getOperand()
        ).orElse(null);

        if (promotedType == null) {
            return ctUnaryOperator;
        }

        if (this.shouldApplyOnUnaryOperator.shouldApplyOn(ctUnaryOperator, ctUnaryOperator.getOperand())) {
            ctUnaryOperator.setOperand(ExpressionUtil.castExpression(promotedType, ctUnaryOperator.getOperand()));
        }

        return ctUnaryOperator;
    }

    @FunctionalInterface
    public interface OperatorPredicate<T extends CtExpression<?>> {
        boolean shouldApplyOn(T operator, CtExpression<?> ctExpression);
    }
}
