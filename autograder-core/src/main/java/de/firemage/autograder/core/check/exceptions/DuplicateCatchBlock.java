package de.firemage.autograder.core.check.exceptions;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.check.structure.DuplicateCode;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.VariableUtil;
import de.firemage.autograder.core.integrated.structure.StructuralEqualsVisitor;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTry;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtCatchVariableReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ExecutableCheck(reportedProblems = { ProblemType.DUPLICATE_CATCH_BLOCK })
public class DuplicateCatchBlock extends IntegratedCheck {
    public static boolean isDuplicateBlock(CtStatement left, CtStatement right, Predicate<? super StructuralEqualsVisitor.Difference> isAllowedDifference) {
        // We initially reject any difference, whether a difference is fine, will later be checked
        StructuralEqualsVisitor visitor = new StructuralEqualsVisitor((ctRole, element) -> false);

        // don't emit a problem if the catch block was already a duplicate of another block flagged by the duplicate code check
        if (DuplicateCode.isConsideredDuplicateCode(List.of(left), List.of(right))) {
            return false;
        }

        if (visitor.checkEquals(left, right)) {
            return true;
        }

        for (var difference : visitor.differences()) {
            if (!isAllowedDifference.test(difference)) {
                return false;
            }
        }

        return true;
    }

    // The check allows things to differ if they are associated with the catch variable.
    // For example between two catches, the caught exception type will differ, which is allowed.
    // Additionally, simple variable renames are allowed.
    private static boolean isAssociatedWithVariable(CtCatchVariable<?> ctCatchVariable, CtElement ctElement) {
        // this is executed if the type differs:
        if (ctElement.getParent() instanceof CtCatchVariableReference<?> ctVariableReference) {
            return ctCatchVariable.equals(VariableUtil.getVariableDeclaration(ctVariableReference));
        }

        // this is when the variable names differ:
        if (ctElement instanceof CtCatchVariableReference<?> ctVariableReference) {
            return ctCatchVariable.equals(VariableUtil.getVariableDeclaration(ctVariableReference));
        }

        return false;
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtTry>() {
            @Override
            public void process(CtTry ctTry) {
                if (ctTry.isImplicit() || !ctTry.getPosition().isValidPosition()) {
                    return;
                }

                List<CtCatch> catchers = ctTry.getCatchers();
                // this likely happens for try-with blocks without a catch:
                if (catchers.size() < 2) {
                    return;
                }

                Set<CtElement> processedCatchers = Collections.newSetFromMap(new IdentityHashMap<>(catchers.size()));
                for (CtCatch firstCatch : catchers) {
                    var ctVariable = firstCatch.getParameter();
                    Collection<CtCatch> exceptions = new ArrayList<>(List.of(firstCatch));

                    for (CtCatch ctCatch : catchers) {
                        Predicate<StructuralEqualsVisitor.Difference> isAllowedDifference = difference -> difference.role() == CtRole.NAME
                            && difference.left() instanceof CtElement leftElement
                            && difference.right() instanceof CtElement rightElement
                            && isAssociatedWithVariable(ctVariable, leftElement)
                            && isAssociatedWithVariable(ctCatch.getParameter(), rightElement);

                        if (ctCatch == firstCatch
                            // don't emit a problem if the catch block was already a duplicate of another block
                            || processedCatchers.contains(ctCatch)
                            || !isDuplicateBlock(firstCatch.getBody(), ctCatch.getBody(), isAllowedDifference)) {
                            continue;
                        }

                        exceptions.add(ctCatch);
                    }

                    if (exceptions.size() > 1) {
                        processedCatchers.addAll(exceptions);
                        addLocalProblem(
                            ctTry,
                            new LocalizedMessage("common-reimplementation", Map.of(
                                "suggestion", "try { ... } catch (%s %s) { ... }".formatted(
                                    exceptions.stream()
                                        .map(CtCatch::getParameter)
                                        .map(CtCatchVariable::getMultiTypes)
                                        .flatMap(List::stream)
                                        .map(CtElement::toString)
                                        .collect(Collectors.joining(" | ")),
                                    ctVariable.getSimpleName()
                                )
                            )),
                            ProblemType.DUPLICATE_CATCH_BLOCK
                        );
                    }
                }
            }
        });
    }
}
