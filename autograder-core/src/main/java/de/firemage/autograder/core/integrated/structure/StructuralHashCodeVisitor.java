package de.firemage.autograder.core.integrated.structure;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.function.BiPredicate;

/**
 * A visitor that computes a hash code for a spoon element based on its structure.
 * <br>
 * The default hashCode implementation in spoon does not ignore comments or naming of things
 * and to be compatible with the {@link StructuralEqualsVisitor} a different hashCode implementation is needed.
 * The hash code visitor tries to be as generic as possible, but with a low number of collisions.
 */
public final class StructuralHashCodeVisitor extends CtScanner {
    private final HashCodeBuilder builder;
    private final BiPredicate<? super CtRole, Object> isAllowedDifference;

    private StructuralHashCodeVisitor(BiPredicate<? super CtRole, Object> isAllowedDifference) {
        this.builder = new HashCodeBuilder();
        this.isAllowedDifference = isAllowedDifference;
    }

    public static int computeHashCode(CtElement element, BiPredicate<? super CtRole, Object> isAllowedDifference) {
        StructuralHashCodeVisitor visitor = new StructuralHashCodeVisitor(isAllowedDifference);
        visitor.scan(element);
        return visitor.getHashCode();
    }

    @Override
    public void enter(CtElement ctElement) {
        if (ctElement instanceof CtTypeReference<?> ctTypeReference) {
            this.builder.append(ctTypeReference.getSimpleName());
        }

        this.builder.append(ctElement.getClass().getSimpleName().hashCode());
    }

    @Override
    public void scan(CtRole ctRole, CtElement element) {
        if (StructuralEqualsVisitor.shouldSkip(ctRole, element, this.isAllowedDifference)) {
            return;
        }
        this.builder.append(ctRole);
        super.scan(ctRole, element);
    }

    public int getHashCode() {
        return this.builder.toHashCode();
    }
}
