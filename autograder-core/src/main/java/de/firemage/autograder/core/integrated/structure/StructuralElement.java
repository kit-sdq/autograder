package de.firemage.autograder.core.integrated.structure;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;

import java.util.function.BiPredicate;

public record StructuralElement<T extends CtElement>(T element, BiPredicate<? super CtRole, Object> isAllowedDifference) {
    public static <T extends CtElement> StructuralElement<T> of(T element, BiPredicate<? super CtRole, Object> isAllowedDifference) {
        return new StructuralElement<>(element, isAllowedDifference);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }

        if (!(otherObject instanceof StructuralElement<?>(var otherElement, var otherIsAllowedDifference))) {
            return false;
        }

        return StructuralEqualsVisitor.equals(this.element, otherElement, (role, element) -> {
            // TODO: && or || here?
            return this.isAllowedDifference.test(role, element) && otherIsAllowedDifference.test(role, element);
        });
    }

    @Override
    public int hashCode() {
        return StructuralHashCodeVisitor.computeHashCode(this.element, this.isAllowedDifference);
    }
}
