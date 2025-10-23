package de.firemage.autograder.core.check.general;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.ElementUtil;
import de.firemage.autograder.core.integrated.MethodHierarchy;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.MethodUtil;
import de.firemage.autograder.core.integrated.UsesFinder;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ExecutableCheck(reportedProblems = {
    ProblemType.USE_DIFFERENT_VISIBILITY,
    ProblemType.USE_DIFFERENT_VISIBILITY_PEDANTIC,
    ProblemType.USE_DIFFERENT_VISIBILITY_PUBLIC_FIELD
})
public class UseDifferentVisibility extends IntegratedCheck {
    private enum Visibility implements Comparable<Visibility> {
        PRIVATE,
        DEFAULT,
        PROTECTED,
        PUBLIC;

        static Visibility of(CtModifiable ctModifiable) {
            if (ctModifiable.isPublic()) {
                return PUBLIC;
            } else if (ctModifiable.isProtected()) {
                return PROTECTED;
            } else if (ctModifiable.isPrivate()) {
                return PRIVATE;
            } else {
                return DEFAULT;
            }
        }

        boolean isMoreRestrictiveThan(Visibility other) {
            // this < other
            return this.compareTo(other) < 0;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static Visibility getVisibility(CtTypeMember ctTypeMember) {
        CtModel ctModel = ctTypeMember.getFactory().getModel();

        if (ctTypeMember.getParent() instanceof CtInterface<?>) {
            // in interfaces, all members are implicitly public
            return Visibility.PUBLIC;
        }

        Stream<CtElement> referencesStream = UsesFinder.getAllUses(ctTypeMember);
        if (ctTypeMember instanceof CtMethod<?> method) {
            // For methods, we also consider overriding methods
            referencesStream = Stream.concat(referencesStream, MethodHierarchy
                    .streamAllOverridingMethods(method)
                    .map(MethodHierarchy.MethodOrLambda::getExecutable));
        }
        List<CtElement> references = referencesStream.toList();

        CtElement commonParent = ElementUtil.findCommonParent(ctTypeMember, references);
        CtType<?> declaringType = ctTypeMember.getDeclaringType();

        // if there are no references, the member itself will be returned
        if (ctTypeMember == commonParent) {
            return Visibility.of(ctTypeMember);
        }

        // if all references are in the same type as the member, it can be private
        if (commonParent instanceof CtType<?> ctType
            && (ctType.equals(declaringType)
            // special case for inner classes
            || ctTypeMember.getTopLevelType().equals(ctType))) {
            // You can not override private methods. They are still in the same class, so the next visibility is default.
            // See the "testOverrideEnum" for an example of when this can happen.
            if (ctTypeMember instanceof CtMethod<?> ctMethod && MethodHierarchy.isOverriddenMethod(ctMethod)) {
                return Visibility.DEFAULT;
            }

            return Visibility.PRIVATE;
        }

        if (commonParent instanceof CtPackage ctPackage && ctPackage.equals(ctModel.getRootPackage())) {
            // handle the case where all references are in the default package
            if (ctTypeMember.getParent(CtPackage.class).equals(ctPackage)
                && references.stream().allMatch(ref -> ref.getParent(CtPackage.class).equals(ctPackage))) {
                return Visibility.DEFAULT;
            }

            return Visibility.PUBLIC;
        }

        return Visibility.of(ctTypeMember);
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtTypeMember>() {
            @Override
            public void process(CtTypeMember ctTypeMember) {
                if (!ctTypeMember.getPosition().isValidPosition()
                    || ctTypeMember.isImplicit()
                    || ctTypeMember.isPrivate()) {
                    return;
                }

                Visibility currentVisibility = Visibility.of(ctTypeMember);
                if (ctTypeMember instanceof CtMethod<?> ctMethod && (MethodUtil.isMainMethod(ctMethod) || MethodHierarchy.isOverridingMethod(ctMethod))) {
                    return;
                }

                // skip enum values for which one cannot change the visibility
                if (ctTypeMember instanceof CtEnumValue<?>) {
                    return;
                }

                // only check methods and fields
                Visibility visibility;
                if (ctTypeMember instanceof CtMethod<?> ctMethod) {
                    visibility = getVisibility(ctMethod);
                } else if (ctTypeMember instanceof CtField<?> ctField) {
                    visibility = getVisibility(ctField);

                    // special case for fields that are referenced by other fields in the same class
                    // For more details see the test case TestUseDifferentVisibility#testBackwardReference
                    Optional<Visibility> referencingVisibility = ctField.getDeclaringType()
                        .getFields()
                        .stream()
                        // filter out the field itself and those that do not reference the field
                        .filter(field -> field != ctField && UsesFinder.variableUses(ctField).nestedIn(field).hasAny())
                        .map(UseDifferentVisibility::getVisibility)
                        .max(Visibility::compareTo);

                    if (referencingVisibility.isPresent() && visibility.isMoreRestrictiveThan(referencingVisibility.get())) {
                        visibility = referencingVisibility.get();
                    }
                } else {
                    return;
                }

                if (visibility.isMoreRestrictiveThan(currentVisibility)) {
                    // it does not make sense to deduct for public things that should be default visibility,
                    // so they are emitted as a different problem type (pedantic)
                    ProblemType problemType = ProblemType.USE_DIFFERENT_VISIBILITY_PEDANTIC;
                    if (visibility == Visibility.PRIVATE) {
                        problemType = ProblemType.USE_DIFFERENT_VISIBILITY;
                    }

                    // do not suggest to change the visibility of public methods if there is no main method in the model
                    if (!staticAnalysis.getCodeModel().hasMainMethod() && !ctTypeMember.getDeclaringType().isPrivate()) {
                        return;
                    }

                    addLocalProblem(
                        ctTypeMember,
                        new LocalizedMessage(
                            "use-different-visibility",
                            Map.of(
                                "name", ctTypeMember.getSimpleName(),
                                "suggestion", visibility.toString()
                            )
                        ),
                        problemType
                    );
                } else if (ctTypeMember instanceof CtField<?> ctField
                    && (currentVisibility == Visibility.PUBLIC || currentVisibility == Visibility.DEFAULT)
                    && (!ctField.isStatic() || !ctField.isFinal())) {
                    addLocalProblem(
                        ctField,
                        new LocalizedMessage(
                            "use-different-visibility-field",
                            Map.of(
                                "name", ctField.getSimpleName(),
                                "suggestion", Visibility.PRIVATE.toString()
                            )
                        ),
                        ProblemType.USE_DIFFERENT_VISIBILITY_PUBLIC_FIELD
                    );
                }
            }
        });
    }

    @Override
    public Optional<Integer> maximumProblems() {
        return Optional.of(6);
    }
}
