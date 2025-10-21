package de.firemage.autograder.core.integrated.evaluator;

import de.firemage.autograder.core.integrated.evaluator.algebra.ApplyShortCircuit;
import de.firemage.autograder.core.integrated.evaluator.fold.ChainedFold;
import de.firemage.autograder.core.integrated.evaluator.fold.DeduplicateOperatorApplication;
import de.firemage.autograder.core.integrated.evaluator.fold.EvaluateLiteralOperations;
import de.firemage.autograder.core.integrated.evaluator.fold.EvaluatePartialLiteralOperations;
import de.firemage.autograder.core.integrated.evaluator.fold.Fold;
import de.firemage.autograder.core.integrated.evaluator.fold.InferOperatorTypes;
import de.firemage.autograder.core.integrated.evaluator.fold.InlineVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.eval.PartialEvaluator;
import spoon.reflect.visitor.CtScanner;

public class Evaluator extends CtScanner implements PartialEvaluator {
    private CtElement root;
    private final Fold fold;

    public Evaluator(Fold fold) {
        this.fold = fold;
    }

    public Evaluator(Fold firstFold, Fold... other) {
        this(ChainedFold.chain(firstFold, other));
    }

    public Evaluator() {
        this(InferOperatorTypes.create(),
            InlineVariableRead.create(),
            DeduplicateOperatorApplication.create(),
            ApplyShortCircuit.create(),
            EvaluatePartialLiteralOperations.create(),
            EvaluateLiteralOperations.create()
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends CtElement> R evaluate(R ctElement) {
        // the clone detaches the element from the model
        //
        // any modifications must not affect the model
        R result = this.evaluateUnsafe((R) ctElement.clone());

        if (result.isParentInitialized() && result.getParent() != null) {
            throw new IllegalStateException("The parent of the root node has not been detached");
        }

        return result;
    }

    /**
     * Works like {@link #evaluate(CtElement)} but does not clone the input element.
     *
     * @param ctElement the element to evaluate
     * @return the evaluated element
     * @param <R> the type of the element
     */
    @SuppressWarnings("unchecked")
    private <R extends CtElement> R evaluateUnsafe(R ctElement) {
        this.root = ctElement;

        ctElement.accept(this);

        return (R) this.root;
    }

    private void setResult(CtElement result, CtElement ctElement) {
        // do not replace the node if it has not been changed
        if (result == ctElement) return;

        // To preserve the integrity of the model, where each element points to the correct parent,
        // we have to clone the result before replacing the original element.
        CtElement replacement = result.clone();

        // to replace a node in the tree, the parent must be initialized
        //
        // the root node is detached from the model, so it has no parent
        // if the root node has been updated, it will be replaced:
        if (ctElement == this.root) {
            // the node that will replace the root, might have a parent that is below the root
            // so we need to detach the new root from its parent to prevent weird cycles
            replacement.setParent(null);
            this.root = replacement;
        } else {
            ctElement.replace(replacement);
        }
    }

    @Override
    protected void enter(CtElement ctElement) {
        this.setResult(this.fold.enter(ctElement), ctElement);
    }

    // exit is called when the scanner exits a node
    // the scanner calls enter first on the parent and traverses all children
    // the children should be evaluated before the parent, so the fold is called in exit
    @Override
    protected void exit(CtElement ctElement) {
        this.setResult(this.fold.exit(ctElement), ctElement);
    }
}
