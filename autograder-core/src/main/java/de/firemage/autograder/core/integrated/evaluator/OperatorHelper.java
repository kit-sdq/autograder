package de.firemage.autograder.core.integrated.evaluator;

import de.firemage.autograder.core.integrated.ExpressionUtil;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;

import java.util.Optional;
import java.util.Set;

public final class OperatorHelper {
    private OperatorHelper() {
    }

    /**
     * Gets the representation of the operator in the source code. For example, POS will return "+".
     *
     * @param operatorKind the operator
     * @return java source code representation of a pre or post unary operator.
     */
    public static String getOperatorText(UnaryOperatorKind operatorKind) {
        return switch (operatorKind) {
            case POS -> "+";
            case NEG -> "-";
            case NOT -> "!";
            case COMPL -> "~";
            case PREINC -> "++";
            case PREDEC -> "--";
            case POSTINC -> "++";
            case POSTDEC -> "--";
        };
    }

    /**
     * Gets the representation of the operator in the source code. For example, OR will return "||".
     *
     * @param operatorKind the operator
     * @return java source code representation of a binary operator.
     */
    public static String getOperatorText(BinaryOperatorKind operatorKind) {
        return switch (operatorKind) {
            case OR -> "||";
            case AND -> "&&";
            case BITOR -> "|";
            case BITXOR -> "^";
            case BITAND -> "&";
            case EQ -> "==";
            case NE -> "!=";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case SL -> "<<";
            case SR -> ">>";
            case USR -> ">>>";
            case PLUS -> "+";
            case MINUS -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case INSTANCEOF -> "instanceof";
        };
    }

    private static final Set<Class<?>> WHOLE_NUMBERS = Set.of(
        byte.class,
        short.class,
        int.class,
        long.class
    );

    private static final Set<Class<?>> NUMBERS_PROMOTED_TO_INT = Set.of(
        byte.class,
        short.class,
        char.class
    );

    private static boolean isIntegralType(CtTypeReference<?> ctTypeReference) {
        return ctTypeReference.isPrimitive()
            // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.2.1
            && (WHOLE_NUMBERS.contains(ctTypeReference.getActualClass()) || ctTypeReference.getActualClass().equals(char.class));
    }

    private static boolean isNumericType(CtTypeReference<?> ctTypeReference) {
        return ctTypeReference.isPrimitive() && !ctTypeReference.getActualClass().equals(boolean.class);
    }

    /**
     * When using an unary-operator on an operand, the operand type might be changed before the operator is applied.
     * For example, the result of {@code ~((short) 1)} will be of type {@code int} and not {@code short}.
     *
     * @param operandType the operand to apply the operator on
     * @return the type after applying the operator or {@link Optional#empty()} if promotion does not apply
     */
    private static Optional<CtTypeReference<?>> unaryNumericPromotion(CtTypeReference<?> operandType) {
        // if the operand is of type Byte, Short, Character, Integer, Long, Float, or Double it is subject
        // to unboxing (§5.1.8)
        operandType = operandType.unbox();

        // check if unary numeric promotion applies
        if (!isNumericType(operandType)) {
            return Optional.empty();
        }

        // if the operand is of type byte, short, or char, it is promoted to a value of type int by a widening
        // primitive conversion (§5.1.2).
        if (NUMBERS_PROMOTED_TO_INT.contains(operandType.getActualClass())) {
            return Optional.of(operandType.getFactory().Type().integerPrimitiveType());
        }

        // otherwise, the operand is not converted at all.
        return Optional.of(operandType);
    }

    private static Optional<CtTypeReference<?>> binaryNumericPromotion(
        CtTypeReference<?> leftType,
        CtTypeReference<?> rightType
    ) {
        TypeFactory typeFactory = leftType.getFactory().Type();
        // If any operand is of a reference type, it is subjected to unboxing conversion (§5.1.8).
        leftType = leftType.unbox();
        rightType = rightType.unbox();

        // each of which must denote a value that is convertible to a numeric type
        if (!isNumericType(leftType) || !isNumericType(rightType)) {
            return Optional.empty();
        }

        CtTypeReference<?> doubleType = typeFactory.doublePrimitiveType();
        // If either operand is of type double, the other is converted to double.
        if (leftType.equals(doubleType) || rightType.equals(doubleType)) {
            return Optional.of(doubleType);
        }

        // Otherwise, if either operand is of type float, the other is converted to float.
        CtTypeReference<?> floatType = typeFactory.floatPrimitiveType();
        if (leftType.equals(floatType) || rightType.equals(floatType)) {
            return Optional.of(floatType);
        }

        // Otherwise, if either operand is of type long, the other is converted to long.
        CtTypeReference<?> longType = typeFactory.longPrimitiveType();
        if (leftType.equals(longType) || rightType.equals(longType)) {
            return Optional.of(longType);
        }

        // Otherwise, both operands are converted to type int.
        return Optional.of(typeFactory.integerPrimitiveType());
    }

    /**
     * Get the promoted type of the binary operator, as defined by the Java Language Specification.
     * <p>
     * Before an operator is applied, the type of the operands might be changed.
     * This is called <i>promotion</i>.
     * For example {@code 1 + 1.0} has an int and a double as operands.
     * The left operand is promoted to a double, so that the left and right operand have the same type.
     *
     * @param operator the operator
     * @param left the left operand, {@link CtExpression#getFactory()} must not return {@code null}.
     * @param right the right operand
     * @return the promoted type or {@link Optional#empty()} if promotion does not apply or the operation is invalid.
     *         Not every operator is defined for every combination of operands.
     *         For example {@code 1 << 1.0} is invalid.
     *         In this case, {@link Optional#empty()} is returned.
     * @throws UnsupportedOperationException if the operator is {@link BinaryOperatorKind#INSTANCEOF} or an unknown operator.
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.6.2">JLS 5.6.2</a>
     */
    public static Optional<CtTypeReference<?>> getPromotedType(
        BinaryOperatorKind operator,
        CtExpression<?> left,
        CtExpression<?> right
    ) {
        TypeFactory typeFactory = left.getFactory().Type();
        CtTypeReference<?> leftType = ExpressionUtil.getExpressionType(left);
        CtTypeReference<?> rightType = ExpressionUtil.getExpressionType(right);

        switch (operator) {
            // logical operators
            case AND, OR -> {
                CtTypeReference<?> booleanType = typeFactory.booleanPrimitiveType();
                if (!leftType.equals(booleanType) || !rightType.equals(booleanType)) {
                    return Optional.empty();
                }

                return Optional.of(booleanType);
            }

            // shift operators are special:
            case SL, SR, USR -> {
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.19
                // on each operand unary numeric promotion is performed
                CtTypeReference<?> promotedLeft = unaryNumericPromotion(leftType).orElse(null);
                CtTypeReference<?> promotedRight = unaryNumericPromotion(rightType).orElse(null);

                if (promotedLeft == null || promotedRight == null) {
                    return Optional.empty();
                }

                // after promotion, both operands have to be an integral type:
                if (!isIntegralType(promotedLeft) || !isIntegralType(promotedRight)) {
                    return Optional.empty();
                }

                // The type of the shift expression is the promoted type of the left-hand operand.
                return Optional.of(promotedLeft);
            }
            case INSTANCEOF ->
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.20.2
                // Not implemented, because it is not necessary for the current use case.
                throw new UnsupportedOperationException("instanceof is not yet implemented");

            // on the following operators binary numeric promotion is performed:
            case EQ, NE -> {
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.21
                CtTypeReference<?> unboxedLeftType = leftType.unbox();
                CtTypeReference<?> unboxedRightType = rightType.unbox();

                // The equality operators may be used to compare two operands that are convertible (§5.1.8)
                // to numeric type, or two operands of type boolean or Boolean, or two operands that are each
                // of either reference type or the null type. All other cases result in a compile-time error.
                CtTypeReference<?> booleanType = typeFactory.booleanPrimitiveType();
                return binaryNumericPromotion(leftType, rightType).or(() -> {
                    // check if both operands are of type boolean or Boolean
                    // if so they will be promoted to the primitive type boolean
                    if (unboxedLeftType.equals(unboxedRightType) && unboxedLeftType.equals(booleanType)) {
                        return Optional.of(booleanType);
                    }

                    // if both operands are of a reference type
                    if (!unboxedLeftType.isPrimitive() && !unboxedRightType.isPrimitive()) {
                        // It is a compile-time error if it is impossible to convert the type of
                        // either operand to the type of the other by a casting conversion (§5.5).
                        // The run-time values of the two operands would necessarily be unequal
                        // (ignoring the case where both values are null).
                        CtTypeReference<?> nullType = typeFactory.nullType();
                        if (unboxedLeftType.equals(nullType)) {
                            return Optional.of(unboxedRightType);
                        }

                        if (unboxedRightType.equals(nullType)) {
                            return Optional.of(unboxedLeftType);
                        }

                        // In the case where unboxedLeftType or unboxedRightType is a generic type
                        // like the T in `private <T> void foo(T a, T b) { ... }`
                        //
                        // Spoon requires extensive knowledge about the parents of the given element.
                        // (The element would have to have the method as a parent).
                        //
                        // If this is invoked on a type not generated by spoon, the type declaration
                        // might resolve to null, because the parent is missing.
                        // Therefore, this is checked here to prevent it from crashing.

                        if (unboxedLeftType.getTypeDeclaration() != null && unboxedLeftType.isSubtypeOf(unboxedRightType)) {
                            return Optional.of(unboxedRightType);
                        }

                        if (unboxedRightType.getTypeDeclaration() != null && unboxedRightType.isSubtypeOf(unboxedLeftType)) {
                            return Optional.of(unboxedRightType);
                        }

                        return Optional.empty();
                    }

                    return Optional.empty();
                });
            }
            case LT, LE, GT, GE, MUL, DIV, MOD, MINUS -> {
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.20
                return binaryNumericPromotion(leftType, rightType);
            }
            case PLUS -> {
                return binaryNumericPromotion(leftType, rightType).or(() -> {
                    // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.18.1
                    //
                    // If the type of either operand of a + operator is String, then the operation is
                    // string concatenation.
                    CtTypeReference<?> stringType = typeFactory.stringType();
                    if (leftType.equals(stringType) || rightType.equals(stringType)) {
                        return Optional.of(stringType);
                    }

                    return Optional.empty();
                });
            }
            case BITAND, BITXOR, BITOR -> {
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.22
                CtTypeReference<?> unboxedLeftType = leftType.unbox();
                CtTypeReference<?> unboxedRightType = rightType.unbox();

                Set<CtTypeReference<?>> floatingPointNumbers = Set.of(
                    typeFactory.floatPrimitiveType(),
                    typeFactory.doublePrimitiveType()
                );
                if (floatingPointNumbers.contains(unboxedLeftType) || floatingPointNumbers.contains(unboxedRightType)) {
                    return Optional.empty();
                }

                if (unboxedLeftType.equals(unboxedRightType) && unboxedLeftType.equals(typeFactory.booleanPrimitiveType())) {
                    return Optional.of(unboxedLeftType);
                }

                return binaryNumericPromotion(leftType, rightType);
            }
            default -> throw new UnsupportedOperationException("Unknown operator: " + operator);
        }
    }

    /**
     * Gets the promoted type of the unary operator, as defined by the Java Language Specification.
     * <p>
     * Before an operator is applied, the type of the operand might be changed.
     * This is called <i>promotion</i>.
     * For example {@code -((short) 1)} has an operand of type short.
     * The operand is promoted to an int, before the operator is applied.
     *
     * @param operator the operator
     * @param operand the operand, {@link CtExpression#getFactory()} must not return {@code null}.
     * @return the promoted type or {@link Optional#empty()} if promotion does not apply or the operation is invalid.
     *         Not every operator is defined for every combination of operands.
     *         For example {@code !1} is invalid.
     *         In this case, {@link Optional#empty()} is returned.
     * @throws UnsupportedOperationException if the operator is an unknown operator.
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.6.1">JLS 5.6.1</a>
     */
    public static Optional<CtTypeReference<?>> getPromotedType(
        UnaryOperatorKind operator,
        CtExpression<?> operand
    ) {
        TypeFactory typeFactory = operand.getFactory().Type();
        CtTypeReference<?> operandType = ExpressionUtil.getExpressionType(operand);

        // The type of the expression is the type of the variable.
        return switch (operator) {
            case COMPL -> {
                if (isIntegralType(operandType.unbox())) {
                    yield unaryNumericPromotion(operandType);
                }
                yield Optional.empty();
            }
            case POS, NEG ->
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.15.3
                unaryNumericPromotion(operandType);
            case NOT -> {
                if (operandType.unbox().equals(typeFactory.booleanPrimitiveType())) {
                    yield Optional.of(typeFactory.booleanPrimitiveType());
                }
                yield Optional.empty();
            }
            case PREINC, PREDEC, POSTINC, POSTDEC -> {
                // See: https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.15.2
                // (documentation is very similar for all four operators)

                // The type of the operand must be a variable that is convertible to a numeric type.
                if (!(operand instanceof CtVariableRead<?>) || !isNumericType(operandType.unbox())) {
                    yield Optional.empty();
                }
                yield Optional.of(operandType);
            }
        };
    }
}
