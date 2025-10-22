package de.firemage.autograder.core.integrated;

import de.firemage.autograder.core.integrated.evaluator.fold.FoldUtils;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.Arrays;
import java.util.List;

public final class FactoryUtil {
    private FactoryUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> CtLiteral<T> makeLiteralNumber(CtTypeReference<T> ctTypeReference, Number number) {
        Object value = FoldUtils.convert(ctTypeReference, number);

        return makeLiteral(ctTypeReference, (T) value);
    }

    /**
     * Makes a new literal with the given value and type.
     *
     * @param ctTypeReference a reference to the type of the literal
     * @param value the value of the literal
     * @param <T>   the type of the value
     *
     * @return a new literal with the given value, note that the base is not set
     */
    public static <T> CtLiteral<T> makeLiteral(CtTypeReference<T> ctTypeReference, T value) {
        CtLiteral<T> literal = ctTypeReference.getFactory().createLiteral();
        literal.setType(ctTypeReference.clone());
        literal.setValue(value);
        return literal;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> CtBinaryOperator<T> createBinaryOperator(
        CtExpression<?> leftHandOperand,
        CtExpression<?> rightHandOperand,
        BinaryOperatorKind operatorKind
    ) {
        Factory factory = leftHandOperand.getFactory();
        if (factory == null) {
            factory = rightHandOperand.getFactory();
        }

        CtBinaryOperator ctBinaryOperator = factory.createBinaryOperator(
            leftHandOperand.clone(),
            rightHandOperand.clone(),
            operatorKind
        );

        if (ctBinaryOperator.getType() == null) {
            ctBinaryOperator.setType(FoldUtils.inferType(ctBinaryOperator));
        }

        return ctBinaryOperator;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T> CtUnaryOperator<T> createUnaryOperator(UnaryOperatorKind operatorKind, CtExpression<?> ctExpression) {
        CtUnaryOperator ctUnaryOperator = ctExpression.getFactory().createUnaryOperator();
        ctUnaryOperator.setOperand(ctExpression.clone());
        ctUnaryOperator.setKind(operatorKind);

        if (ctUnaryOperator.getType() == null) {
            ctUnaryOperator.setType(FoldUtils.inferType(ctUnaryOperator));
        }

        return ctUnaryOperator;
    }

    /**
     * Creates an operator assignment for the given left-hand side and right-hand side expressions.
     *
     * @param lhs the left-hand side expression, which the right-hand side is assigned to
     * @param rhs the right-hand side expression, which is assigned to the left-hand side
     * @param operatorKind the operator kind to use for the assignment
     * @param <T> the type of the left-hand side expression
     * @param <A> the type of the right-hand side expression
     * @return a new operator assignment
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T, A extends T> CtOperatorAssignment<T, A> createOperatorAssignment(
        CtExpression<?> lhs,
        CtExpression<?> rhs,
        BinaryOperatorKind operatorKind
    ) {
        CtOperatorAssignment ctOperatorAssignment = lhs.getFactory().createOperatorAssignment();

        ctOperatorAssignment.setAssigned(lhs.clone());
        ctOperatorAssignment.setKind(operatorKind);
        ctOperatorAssignment.setAssignment(rhs.clone());

        if (ctOperatorAssignment.getType() == null) {
            ctOperatorAssignment.setType(rhs.getType().clone());
        }

        return ctOperatorAssignment;
    }


    /**
     * Creates a static invocation of the given method on the given target type.
     *
     * @param targetType the type on which the method is defined
     * @param methodName the name of the method
     * @param parameters the parameters to pass to the method
     * @return the invocation
     * @param <T> the result type of the invocation
     */
    public static <T> CtInvocation<T> createStaticInvocation(
        CtTypeReference<?> targetType,
        String methodName,
        CtExpression<?>... parameters
    ) {
        Factory factory = targetType.getFactory();

        CtMethod<T> methodHandle = null;
        List<CtMethod<?>> potentialMethods = targetType.getTypeDeclaration().getMethodsByName(methodName);
        if (potentialMethods.size() == 1) {
            methodHandle = (CtMethod<T>) potentialMethods.get(0);
        } else {
            methodHandle = targetType.getTypeDeclaration().getMethod(
                methodName,
                Arrays.stream(parameters).map(ExpressionUtil::getExpressionType).toArray(CtTypeReference[]::new)
            );
        }

        return factory.createInvocation(
            factory.createTypeAccess(methodHandle.getDeclaringType().getReference()),
            methodHandle.getReference(),
            parameters
        );
    }
}
