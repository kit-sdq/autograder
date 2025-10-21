package de.firemage.autograder.core.integrated.structure;

import spoon.reflect.declaration.CtElement;

public record StructuralElement<T extends CtElement>(T element) {
    public static <T extends CtElement> StructuralElement<T> of(T element) {
        return new StructuralElement<>(element);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }

        if (!(otherObject instanceof StructuralElement<?>(var otherElement))) {
            return false;
        }

        return StructuralEqualsVisitor.equals(this.element, otherElement);
    }

    @Override
    public int hashCode() {
        return StructuralHashCodeVisitor.computeHashCode(this.element);
    }
}
