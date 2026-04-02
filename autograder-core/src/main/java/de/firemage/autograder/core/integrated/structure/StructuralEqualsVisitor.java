package de.firemage.autograder.core.integrated.structure;

import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.VariableUtil;
import de.firemage.autograder.core.integrated.CoreUtil;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.path.CtRole;
import spoon.support.visitor.equals.EqualsVisitor;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.BiPredicate;

public final class StructuralEqualsVisitor extends EqualsVisitor {
    private static final boolean IS_IN_DEBUG_MODE = CoreUtil.isInDebugMode();
    private static final Set<CtRole> ALLOWED_MISMATCHING_ROLES = Set.of(
        // allow mismatching comments
        CtRole.COMMENT, CtRole.COMMENT_CONTENT, CtRole.COMMENT_TAG, CtRole.COMMENT_TYPE
    );

    /**
     * Allows elements to differ in name if they are local variables, fields, parameters or catch variables.
     */
    public static final BiPredicate<CtRole, Object> ALLOW_NAME_DIFFERENCE = (CtRole role, Object element) ->
        role == CtRole.NAME && CoreUtil.isInstanceOfAny(element, CtLocalVariable.class, CtField.class, CtParameter.class, CtCatchVariable.class);

    /**
     * Allows elements to differ in a binary expression if the difference could be refactored into a separate method.
     */
    public static final BiPredicate<CtRole, Object> ALLOW_REFACTORABLE_DIFFERENCE = (CtRole role, Object element) -> {
        if (role != CtRole.LEFT_OPERAND && role != CtRole.RIGHT_OPERAND) {
            return false;
        }

        return element instanceof CtExpression<?> ctExpression && ExpressionUtil.isConstantExpressionOr(ctExpression, e -> {
            // in addition to constant expressions, it is also okay to access parameters (if they have not been modified in the method)
            if (e instanceof CtVariableRead<?> ctVariableRead) {
                CtVariable<?> ctVariable = VariableUtil.getVariableDeclaration(ctVariableRead.getVariable());
                return ctVariable instanceof CtParameter<?> ctParameter && VariableUtil.isEffectivelyFinal(ctParameter);
            }

            return false;
        });
    };

    private final SequencedSet<Difference> differences;
    private final BiPredicate<? super CtRole, Object> isAllowedDifference;

    public record Difference(CtRole role, Object left, Object right) {}

    public static SequencedSet<Difference> findDifferences(CtElement left, CtElement right, BiPredicate<? super CtRole, Object> isAllowedDifference) {
        var visitor = new StructuralEqualsVisitor(isAllowedDifference);
        visitor.checkEquals(left, right);
        return visitor.differences();
    }

    public StructuralEqualsVisitor(BiPredicate<? super CtRole, Object> isAllowedDifference) {
        this.differences = new LinkedHashSet<>();
        this.isAllowedDifference = isAllowedDifference;
    }

    public static boolean equals(CtElement left, CtElement right, BiPredicate<? super CtRole, Object> isAllowedDifference) {
        return new StructuralEqualsVisitor(isAllowedDifference).checkEquals(left, right);
    }


    public static boolean equals(CtElement left, CtElement right) {
        throw new UnsupportedOperationException("Call the equals with isAllowedDifference instead.");
    }

    @Override
    public boolean checkEquals(CtElement left, CtElement right) {
        if (IS_IN_DEBUG_MODE) {
            boolean result = super.checkEquals(left, right);
            int leftHashCode = StructuralHashCodeVisitor.computeHashCode(left, this.isAllowedDifference);
            int rightHashCode = StructuralHashCodeVisitor.computeHashCode(right, this.isAllowedDifference);

            // If two objects are equal according to the equals(Object) method, then calling
            // the hashCode method on each of the two objects must produce the same integer result.
            if (result && leftHashCode != rightHashCode) {
                throw new IllegalStateException("StructuralHashCode is wrong for the equal objects: %s (hashCode=%d), %s (hashCode=%d)".formatted(left, leftHashCode, right, rightHashCode));
            }

            return result;
        }

        return super.checkEquals(left, right);
    }

    public static boolean shouldSkip(CtRole role, Object element, BiPredicate<? super CtRole, Object> isAllowedDifference) {
        if (role == null) {
            return false;
        }

        if (ALLOWED_MISMATCHING_ROLES.contains(role)) {
            return true;
        }

        // NOTE: element might be a collection of CtElements

        return isAllowedDifference.test(role, element);
    }

    @Override
    protected boolean fail(CtRole role, Object element, Object other) {
        this.differences.add(new Difference(role, element, other));

        if (shouldSkip(role, element, this.isAllowedDifference)) {
            return false;
        }

        this.isNotEqual = true;
        this.notEqualRole = role;
        this.notEqualElement = element;
        this.notEqualOther = other;

        return true;
    }

    /**
     * Returns the differences between the two elements.
     *
     * @return the differences
     */
    public SequencedSet<Difference> differences() {
        return new LinkedHashSet<>(this.differences);
    }
}
