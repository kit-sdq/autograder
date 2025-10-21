package de.firemage.autograder.core.integrated;

import de.firemage.autograder.core.check.api.UseEnumValues;
import de.firemage.autograder.core.integrated.evaluator.Evaluator;
import de.firemage.autograder.core.integrated.evaluator.fold.FoldUtils;
import de.firemage.autograder.core.integrated.evaluator.fold.InlineVariableRead;
import de.firemage.autograder.core.integrated.evaluator.fold.RemoveRedundantCasts;
import spoon.reflect.CtModel;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtTextBlock;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.LiteralBase;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.eval.PartialEvaluator;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class ExpressionUtil {
    private ExpressionUtil() {
    }

    public static Optional<CtTypeReference<?>> isToStringCall(CtExpression<?> expression) {
        if (!TypeUtil.isString(expression.getType())) {
            return Optional.empty();
        }

        if (expression instanceof CtInvocation<?> invocation &&
            MethodUtil.isSignatureEqualTo(invocation.getExecutable(), String.class, "toString")) {
            return Optional.of(invocation.getTarget().getType());
        } else {
            return Optional.empty();
        }
    }

    public static boolean isStringLiteral(CtExpression<?> expression, String value) {
        return expression instanceof CtLiteral<?> literal && literal.getValue() != null &&
            literal.getValue().equals(value);
    }

    public static boolean isNullLiteral(CtExpression<?> expression) {
        return resolveConstant(expression) instanceof CtLiteral<?> literal && literal.getValue() == null;
    }

    public static boolean isIntegerLiteral(CtExpression<?> expression, int value) {
        return expression instanceof CtLiteral<?> literal && literal.getValue().equals(value);
    }

    public static boolean isBoolean(CtTypedElement<?> ctTypedElement) {
        CtTypeReference<?> ctTypeReference = ctTypedElement.getType();
        return ctTypeReference != null && TypeUtil.isTypeEqualTo(ctTypeReference, boolean.class, Boolean.class);
    }

    public static Optional<Boolean> tryGetBooleanLiteral(CtExpression<?> expression) {
        if (resolveConstant(expression) instanceof CtLiteral<?> literal
            && literal.getValue() != null
            && isBoolean(literal)) {

            return Optional.of((Boolean) literal.getValue());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<String> tryGetStringLiteral(CtExpression<?> expression) {
        if (resolveConstant(expression) instanceof CtLiteral<?> literal
            && literal.getValue() != null
            && TypeUtil.isTypeEqualTo(literal.getType(), String.class)) {

            return Optional.of((String) literal.getValue());
        } else {
            return Optional.empty();
        }
    }

    // equals impl of CtLiteral seems to be broken
    public static boolean areLiteralsEqual(
        CtLiteral<?> left,
        CtLiteral<?> right
    ) {
        if (left == null && right == null) {
            return true;
        } else if (left == null || right == null) {
            return false;
        }

        if (left.getValue() == null) {
            return right.getValue() == null;
        } else if (right.getValue() == null) {
            return false;
        }

        if (left.getValue() instanceof Character l && right.getValue() instanceof Character r) {
            return l.equals(r);
        } else if (left.getValue() instanceof Number l && right.getValue() instanceof Character r) {
            return l.intValue() == (int) r;
        } else if (left.getValue() instanceof Character l && right.getValue() instanceof Number r) {
            return (int) l == r.intValue();
        }

        if (!(left.getValue() instanceof Number valLeft)
            || !(right.getValue() instanceof Number valRight)) {
            return left.getValue() == right.getValue() || left.getValue().equals(right.getValue());
        }

        if (valLeft instanceof Float || valLeft instanceof Double || valRight instanceof Float
            || valRight instanceof Double) {
            return valLeft.doubleValue() == valRight.doubleValue();
        }

        return valLeft.longValue() == valRight.longValue();
    }

    public static List<CtExpression<?>> getElementsOfExpression(CtExpression<?> ctExpression) {
        var supportedCollections = Stream.of(
            List.class,
            java.util.Set.class,
            java.util.Collection.class
        ).map((Class<?> e) -> ctExpression.getFactory().Type().createReference(e));

        List<CtExpression<?>> result = new ArrayList<>();

        CtTypeReference<?> expressionType = ctExpression.getType();
        if (supportedCollections.noneMatch(ty -> ty.equals(expressionType) || expressionType.isSubtypeOf(ty))) {
            return result;
        }

        if (ctExpression instanceof CtInvocation<?> ctInvocation
            && ctInvocation.getTarget() instanceof CtTypeAccess<?>) {
            CtExecutableReference<?> ctExecutableReference = ctInvocation.getExecutable();
            if (ctExecutableReference.getSimpleName().equals("of")) {
                result.addAll(ctInvocation.getArguments());
            }
        }

        return result;
    }

    public static <T> CtLiteral<T> minimumValue(CtLiteral<T> ctLiteral) {
        CtLiteral result = ctLiteral.getFactory().createLiteral();
        result.setBase(LiteralBase.DECIMAL);
        result.setType(ctLiteral.getType().clone());

        // - byte
        // - short
        // - int
        // - long
        // - float
        // - double
        // - boolean
        // - char

        Object value = ctLiteral.getValue();
        Map<Class<?>, Object> minimumValueMapping = Map.ofEntries(
            Map.entry(byte.class, Byte.MIN_VALUE),
            Map.entry(Byte.class, Byte.MIN_VALUE),
            Map.entry(short.class, Short.MIN_VALUE),
            Map.entry(Short.class, Short.MIN_VALUE),
            Map.entry(int.class, Integer.MIN_VALUE),
            Map.entry(Integer.class, Integer.MIN_VALUE),
            Map.entry(long.class, Long.MIN_VALUE),
            Map.entry(Long.class, Long.MIN_VALUE),
            Map.entry(float.class, Float.MIN_VALUE),
            Map.entry(Float.class, Float.MIN_VALUE),
            Map.entry(double.class, Double.MIN_VALUE),
            Map.entry(Double.class, Double.MIN_VALUE),
            Map.entry(boolean.class, false),
            Map.entry(Boolean.class, false),
            Map.entry(char.class, Character.MIN_VALUE),
            Map.entry(Character.class, Character.MIN_VALUE)
        );

        result.setValue(minimumValueMapping.get(value.getClass()));

        return result;
    }

    public static <T> CtLiteral<T> maximumValue(CtLiteral<T> ctLiteral) {
        CtLiteral result = ctLiteral.getFactory().createLiteral();
        result.setBase(LiteralBase.DECIMAL);
        result.setType(ctLiteral.getType().clone());

        // - byte
        // - short
        // - int
        // - long
        // - float
        // - double
        // - boolean
        // - char

        Object value = ctLiteral.getValue();
        Map<Class<?>, Object> maximumValueMapping = Map.ofEntries(
            Map.entry(byte.class, Byte.MAX_VALUE),
            Map.entry(Byte.class, Byte.MAX_VALUE),
            Map.entry(short.class, Short.MAX_VALUE),
            Map.entry(Short.class, Short.MAX_VALUE),
            Map.entry(int.class, Integer.MAX_VALUE),
            Map.entry(Integer.class, Integer.MAX_VALUE),
            Map.entry(long.class, Long.MAX_VALUE),
            Map.entry(Long.class, Long.MAX_VALUE),
            Map.entry(float.class, Float.MAX_VALUE),
            Map.entry(Float.class, Float.MAX_VALUE),
            Map.entry(double.class, Double.MAX_VALUE),
            Map.entry(Double.class, Double.MAX_VALUE),
            Map.entry(boolean.class, true),
            Map.entry(Boolean.class, true),
            Map.entry(char.class, Character.MAX_VALUE),
            Map.entry(Character.class, Character.MAX_VALUE)
        );

        result.setValue(maximumValueMapping.get(value.getClass()));

        return result;
    }

    /**
     * Swaps the operands of a binary operator.
     *
     * @param ctBinaryOperator the operator to swap, can be of any kind
     * @return the cloned version with the operands swapped or the given operator if it is not supported
     * @param <T> the type the operator evaluates to
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T> CtBinaryOperator<T> swapCtBinaryOperator(CtBinaryOperator<T> ctBinaryOperator) {
        CtBinaryOperator result = ctBinaryOperator.clone();

        CtExpression<?> left = result.getLeftHandOperand();
        CtExpression<?> right = result.getRightHandOperand();

        // NOTE: this only implements a few cases, for other non-commutative operators, this will break code
        result.setKind(switch (ctBinaryOperator.getKind()) {
            // a < b => b > a
            case LT -> BinaryOperatorKind.GT;
            // a <= b => b >= a
            case LE -> BinaryOperatorKind.GE;
            // a >= b => b <= a
            case GE -> BinaryOperatorKind.LE;
            // a > b => b < a
            case GT -> BinaryOperatorKind.LT;
            default -> ctBinaryOperator.getKind();
        });

        // swap the left and right
        result.setLeftHandOperand(right);
        result.setRightHandOperand(left);

        return result;
    }

    /**
     * Replaces {@link spoon.reflect.code.CtVariableRead} in the provided expression if they are effectively final
     * and their value is known.
     *
     * @param ctExpression the expression to resolve. If it is {@code null}, then {@code null} is returned
     * @return the resolved expression. It will be cloned and detached from the {@link CtModel}
     * @param <T> the type of the expression
     */
    public static <T> CtExpression<T> resolveConstant(CtExpression<T> ctExpression) {
        if (ctExpression == null) return null;

        Evaluator evaluator = new Evaluator(InlineVariableRead.create(true));

        return evaluator.evaluate(ctExpression);
    }

    /**
     * Converts a binary operator like &lt; to &lt;= or > to >= and adjusts the operands accordingly
     * to make finding patterns on them easier by not having to special-case them. Additionally,
     * one can specify a predicate to swap the operands if necessary. For example, to ensure that
     * a literal is always on the right-hand side.
     *
     * @param shouldSwap the left and right hands are passed to it, and it should return true if they should be swapped and false if nothing should be changed
     * @param ctBinaryOperator the operator to normalize, can be of any kind
     * @return the normalized operator or the given operator if it is not supported
     * @param <T> the type the operator evaluates to
     */
    public static <T> CtBinaryOperator<T> normalizeBy(
        BiPredicate<? super CtExpression<?>, ? super CtExpression<?>> shouldSwap,
        CtBinaryOperator<T> ctBinaryOperator
    ) {
        CtExpression<?> left = ctBinaryOperator.getLeftHandOperand();
        CtExpression<?> right = ctBinaryOperator.getRightHandOperand();

        BinaryOperatorKind operator = ctBinaryOperator.getKind();

        CtBinaryOperator<T> result = ctBinaryOperator.clone();
        result.setKind(operator);
        result.setLeftHandOperand(left.clone());
        result.setRightHandOperand(right.clone());

        // check if the left and right have to be swapped. To do that, the operator must be inverted:
        // a <= b => b >= a
        // a < b => b > a
        // a >= b => b <= a
        // a > b => b < a
        //
        // ^ in this example it is expected that the b should be on the left
        if (shouldSwap.test(left, right)) {
            result = swapCtBinaryOperator(result);
        }

        // in this step < and > are adjusted to <= and >= :
        // a < b => a <= b - 1
        // a > b => a >= b + 1

        return ExpressionUtil.normalize(result);
    }

    /**
     * Converts a binary operator like 'a < b' to 'a <= b - 1' or 'a > b' to 'a >= b + 1'.
     *
     * @param ctBinaryOperator the operator to normalize, can be of any kind
     * @return the normalized operator or the given operator if it is not supported
     * @param <T> the type the operator evaluates to
     */
    private static <T> CtBinaryOperator<T> normalize(CtBinaryOperator<T> ctBinaryOperator) {
        // the following primitive types exist:
        // - byte
        // - short
        // - int
        // - long
        // - float
        // - double
        // - boolean
        // - char
        //
        // of those the following are not `Number`:
        // - boolean
        // - char

        if (!Set.of(BinaryOperatorKind.LT, BinaryOperatorKind.GT).contains(ctBinaryOperator.getKind())
            || !ctBinaryOperator.getRightHandOperand().getType().isPrimitive()) {
            return ctBinaryOperator;
        }

        // the literal to add/subtract. Simply setting it to 1 is not enough, because
        // 1 is of type int and the other side might for example be a double or float
        CtLiteral step = ctBinaryOperator.getFactory().Core().createLiteral();

        Predicate<CtTypeReference<?>> isCharacter = ty -> TypeUtil.isTypeEqualTo(ty, char.class, java.lang.Character.class);
        if (isCharacter.test(ctBinaryOperator.getRightHandOperand().getType())) {
            // for character use an integer literal
            step.setValue((char) 1);
            step.setType(ctBinaryOperator.getFactory().Type().characterPrimitiveType());
        } else {
            // this assumes that < and > are only used with numbers
            step.setValue(FoldUtils.convert(ctBinaryOperator.getRightHandOperand().getType(), ((Number) 1).doubleValue()));
            step.setType(ctBinaryOperator.getRightHandOperand().getType());
        }

        CtBinaryOperator<T> result = ctBinaryOperator.clone();
        if (ctBinaryOperator.getKind() == BinaryOperatorKind.LT) {
            // <lhs> < <rhs> => <lhs> <= <rhs> - 1
            result.setKind(BinaryOperatorKind.LE);
            result.setRightHandOperand(FactoryUtil.createBinaryOperator(
                ctBinaryOperator.getRightHandOperand(),
                step,
                BinaryOperatorKind.MINUS
            ));
        } else if (ctBinaryOperator.getKind() == BinaryOperatorKind.GT) {
            // <lhs> > <rhs> => <lhs> >= <rhs> + 1
            result.setKind(BinaryOperatorKind.GE);
            result.setRightHandOperand(FactoryUtil.createBinaryOperator(
                ctBinaryOperator.getRightHandOperand(),
                step,
                BinaryOperatorKind.PLUS
            ));
        }

        // simplify the resulting operator
        result.setLeftHandOperand(ExpressionUtil.resolveCtExpression(result.getLeftHandOperand()));
        // if the operand was a literal, it might have been promoted
        if (result.getLeftHandOperand() instanceof CtLiteral<?> ctLiteral) {
            result.setLeftHandOperand(ExpressionUtil.castLiteral(
                ExpressionUtil.getExpressionType(ctBinaryOperator.getLeftHandOperand()),
                ctLiteral
            ));
        }

        result.setRightHandOperand(ExpressionUtil.resolveCtExpression(result.getRightHandOperand()));
        if (result.getRightHandOperand() instanceof CtLiteral<?> ctLiteral) {
            result.setRightHandOperand(ExpressionUtil.castLiteral(
                ExpressionUtil.getExpressionType(ctBinaryOperator.getRightHandOperand()),
                ctLiteral
            ));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> CtExpression<T> negate(CtExpression<T> ctExpression) {
        // !(!(a)) => a
        if (ctExpression instanceof CtUnaryOperator<T> ctUnaryOperator && ctUnaryOperator.getKind() == UnaryOperatorKind.NOT) {
            return (CtExpression<T>) ctUnaryOperator.getOperand();
        }

        if (ctExpression instanceof CtBinaryOperator<T> ctBinaryOperator) {
            CtBinaryOperator<T> result = ctBinaryOperator.clone();
            switch (ctBinaryOperator.getKind()) {
                // !(a == b) -> a != b
                case EQ -> {
                    result.setKind(BinaryOperatorKind.NE);
                    return result;
                }
                // !(a != b) -> a == b
                //
                // a | b | a ^ b
                // 0 | 0 |   0
                // 0 | 1 |   1
                // 1 | 0 |   1
                // 1 | 1 |   0
                // => !(a ^ b) -> a == b
                case NE, BITXOR -> {
                    result.setKind(BinaryOperatorKind.EQ);
                    return result;
                }
                // !(a && b) -> !a || !b
                case AND -> {
                    result.setKind(BinaryOperatorKind.OR);
                    result.setLeftHandOperand(negate(result.getLeftHandOperand()));
                    result.setRightHandOperand(negate(result.getRightHandOperand()));
                    return result;
                }
                // !(a || b) -> !a && !b
                case OR -> {
                    result.setKind(BinaryOperatorKind.AND);
                    result.setLeftHandOperand(negate(result.getLeftHandOperand()));
                    result.setRightHandOperand(negate(result.getRightHandOperand()));
                    return result;
                }
                // !(a >= b) -> a < b
                case GE -> {
                    result.setKind(BinaryOperatorKind.LT);
                    return result;
                }
                // !(a > b) -> a <= b
                case GT -> {
                    result.setKind(BinaryOperatorKind.LE);
                    return result;
                }
                // !(a <= b) -> a > b
                case LE -> {
                    result.setKind(BinaryOperatorKind.GT);
                    return result;
                }
                // !(a < b) -> a >= b
                case LT -> {
                    result.setKind(BinaryOperatorKind.GE);
                    return result;
                }
            }
        }

        return FactoryUtil.createUnaryOperator(UnaryOperatorKind.NOT, ctExpression.clone());
    }

    public static <T> CtExpression<T> resolveCtExpression(CtExpression<T> ctExpression) {
        if (ctExpression == null) return null;

        // Spoon's partiallyEvaluate is broken, not configurable, and fixing it would be too much work.
        // Therefore, we use our own implementation.
        PartialEvaluator evaluator = new Evaluator();

        return evaluator.evaluate(ctExpression);
    }

    public static <T> CtExpression<T> castExpression(Class<T> targetType, CtExpression<?> ctExpression) {
        return castExpression(ctExpression.getFactory().Type().createReference(targetType), ctExpression);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, R> CtLiteral<R> castLiteral(CtTypeReference<R> type, CtLiteral<T> literal) {
        CtLiteral result = literal.clone();
        result.setType(type.clone());

        // casting a primitive to a string:
        if (TypeUtil.isTypeEqualTo(type, String.class) && literal.getType().isPrimitive()) {
            result.setValue(literal.getValue().toString());
            return result;
        }

        // It is not possible to cast an Integer to a Double directly, which is a problem.
        CtTypeReference<?> targetType = type.unbox();
        if (targetType.isPrimitive()) {
            // the FoldUtils.convert method only works for Number -> Number conversions
            if (TypeUtil.isSubtypeOf(targetType.box(), Number.class)) {
                // for instances of Number, one can use the convert method:
                if (literal.getValue() instanceof Number number) {
                    result.setValue(FoldUtils.convert(type, number));
                } else {
                    // primitive types that do not implement Number are:
                    // boolean, char

                    // NOTE: it does not make sense to cast a boolean to any other primitive type
                    if (literal.getValue() instanceof Character character) {
                        result.setValue(FoldUtils.convert(type, (int) character));
                    }
                }
            }

            if (TypeUtil.isTypeEqualTo(targetType, char.class)) {
                if (literal.getValue() instanceof Number number) {
                    result.setValue((char) number.intValue());
                } else {
                    result.setValue((char) literal.getValue());
                }
            } else if (TypeUtil.isTypeEqualTo(targetType, boolean.class)) {
                result.setValue((boolean) literal.getValue());
            }
        } else {
            result.setValue(type.getActualClass().cast(literal.getValue()));
        }

        return result;
    }

    // returns the type of the expression after applying the type casts
    public static <T> CtTypeReference<?> getExpressionType(CtExpression<T> ctExpression) {
        CtTypeReference<?> result = ctExpression.getType();

        List<CtTypeReference<?>> typeCasts = ctExpression.getTypeCasts();
        if (!typeCasts.isEmpty()) {
            return typeCasts.getFirst();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends CtExpression<T>> E castExpression(CtTypeReference<T> type, CtExpression<?> ctExpression) {
        // no need to cast if the type is the same
        if (getExpressionType(ctExpression).equals(type)) {
            return (E) ctExpression;
        }

        List<CtTypeReference<?>> typeCasts = new ArrayList<>(ctExpression.getTypeCasts());
        typeCasts.add(0, type.clone());
        ctExpression.setTypeCasts(typeCasts);

        return (E) RemoveRedundantCasts.removeRedundantCasts(ctExpression);
    }

    /**
     * Checks if the given expression is a constant expression, or it satisfies the given predicate.
     *
     * @param expr the expression to check
     * @param isAllowedExpression checks if the expression is allowed to be non-constant
     * @return true if the expression is a constant expression, or it satisfies the given predicate, false otherwise
     * @param <T> the type of the expression
     */
    public static <T> boolean isConstantExpressionOr(CtExpression<T> expr, Predicate<? super CtExpression<?>> isAllowedExpression) {
        var visitor = new CtScanner() {
            private boolean isConstant;
            private boolean isDone;

            private static <T> boolean isConstantVariableRead(CtExpression<T> expression) {
                if (!(expression instanceof CtVariableRead<T> ctVariableRead)) {
                    return false;
                }
                CtVariable<?> ctVariable = VariableUtil.getVariableDeclaration(ctVariableRead.getVariable());

                if (ctVariable == null) {
                    return false;
                }

                return VariableUtil.getEffectivelyFinalExpression(ctVariable).orElse(null) instanceof CtLiteral<?>;
            }

            // use the exit instead of the enter method, so it checks the deepest nodes first.
            // This way, one can assume that when a <left> <op> <right> expression is encountered,
            // the <left> and <right> are already guaranteed to be constant.
            @Override
            protected void exit(CtElement ctElement) {
                if (!(ctElement instanceof CtExpression<?> expression) || this.isDone) {
                    return;
                }

                // Of all the Subinterfaces of CtExpression, these are irrelevant:
                // - CtAnnotation
                // - CtAnnotationFieldAccess
                // - CtOperatorAssignment
                // - CtArrayWrite
                // - CtArrayAccess
                // - CtFieldWrite
                // - CtFieldAccess
                // - CtVariableAccess
                // - CtVariableWrite
                // - CtCodeSnippetExpression (should never be encountered)
                //
                // these might be constant expressions, depending on more context:
                // - CtArrayRead
                // - CtAssignment
                // - CtConstructorCall
                // - CtExecutableReferenceExpression
                // - CtLambda
                // - CtNewArray
                // - CtNewClass
                // - CtSuperAccess
                // - CtSwitchExpression
                // - CtTargetedExpression
                // - CtThisAccess
                // - CtTypePattern
                //
                // and these are the relevant ones:
                // - CtLiteral
                // - CtUnaryOperator
                // - CtBinaryOperator
                // - CtFieldRead (if the field itself is static and final)
                // - CtInvocation (if the method is static and all arguments are constant expressions)
                // - CtTextBlock
                // - CtConditional (ternary operator)
                // - CtTypeAccess

                if (CoreUtil.isInstanceOfAny(
                    expression,
                    CtBinaryOperator.class,
                    UnaryOperator.class,
                    CtTextBlock.class,
                    CtConditional.class,
                    CtTypeAccess.class,
                    CtLiteral.class
                )) {
                    this.isConstant = true;
                    return;
                }

                if (expression instanceof CtInvocation<?> ctInvocation && ctInvocation.getExecutable().isStatic()) {
                    // the arguments and target of the invocation have already been visited and all of them work in a constant context
                    // -> the invocation would work in a constant context as well
                    this.isConstant = true;
                    return;
                }

                if (ExpressionUtil.isEnumValue(expression)) {
                    this.isConstant = true;
                    return;
                }

                if (isConstantVariableRead(expression) || isAllowedExpression.test(expression)) {
                    this.isConstant = true;
                } else {
                    this.isConstant = false;
                    this.isDone = true;
                }
            }
        };

        expr.accept(visitor);

        return visitor.isConstant;
    }

    private static boolean isEnumValue(CtExpression<?> ctExpression) {
        return UseEnumValues.CtEnumFieldRead.of(ctExpression).isPresent();
    }
}
