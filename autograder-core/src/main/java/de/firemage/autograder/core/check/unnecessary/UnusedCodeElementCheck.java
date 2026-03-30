package de.firemage.autograder.core.check.unnecessary;

import de.firemage.autograder.core.CodeModel;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.ElementUtil;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.MethodHierarchy;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.MethodUtil;
import de.firemage.autograder.core.integrated.TypeUtil;
import de.firemage.autograder.core.integrated.UsesFinder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.visitor.CtScanner;

import java.util.Map;
import java.util.Optional;

@ExecutableCheck(reportedProblems = { ProblemType.UNUSED_CODE_ELEMENT, ProblemType.UNUSED_CODE_ELEMENT_PRIVATE })
public class UnusedCodeElementCheck extends IntegratedCheck {
    /**
     * This method implements a number of special cases for elements that we allow to be unused,
     * e.g. methods of a public API (when no main method is present), or parameters mandated by Java's API.
     *
     * @param element
     * @param model
     * @return
     */
    public static boolean isConsideredUnused(CtNamedElement element, CodeModel model) {
        // ignore exception constructors and params in those constructors
        var parentConstructor = ElementUtil.getParentOrSelf(element, CtConstructor.class);
        if (parentConstructor != null && TypeUtil.isSubtypeOf(parentConstructor.getType(), java.lang.Throwable.class)) {
            return false;
        }

        // Special cases for public API if we have no main method:
        if (!model.hasMainMethod()) {
            // ignore unused parameters of non-private methods
            if (element instanceof CtParameter<?> && element.getParent() instanceof CtTypeMember typeMember && !typeMember.getDeclaringType().isPrivate()) {
                return false;
            }

            // ignore unused public type members (i.e. fields, methods, ...)
            if (element instanceof CtTypeMember typeMember && !typeMember.isPrivate() && !typeMember.getDeclaringType().isPrivate()) {
                return false;
            }
        }

        switch (element) {
            case CtVariable<?> variable -> {
                if (UsesFinder.variableUses(variable).hasAny()) {
                    return false;
                }

                if (variable instanceof CtParameter<?> parameter && parameter.getParent() instanceof CtMethod<?> method) {
                    // For method parameters, also look in overriding methods
                    int parameterIndex = ElementUtil.getParameterIndex(parameter, method);
                    return MethodHierarchy
                            .streamAllOverridingMethods(method)
                            .allMatch(m -> isConsideredUnused(m.getExecutable().getParameters().get(parameterIndex), model));
                }

                return true;
            }
            case CtTypeParameter typeParameter -> {
                return UsesFinder.typeParameterUses(typeParameter).hasNone();
            }
            case CtType<?> type -> {
                return UsesFinder.typeUses(type).hasNone();
            }
            case CtExecutable<?> executable -> {
                // Ignore recursive calls
                if (UsesFinder.executableUses(executable).filterIndirectParent(CtMethod.class, m -> m != executable).hasAny()) {
                    return false;
                } else if (executable instanceof CtMethod<?> method) {
                    // For methods, also look for used overriding methods
                    return MethodHierarchy
                            .streamAllOverridingMethods(method)
                            .allMatch(m -> isConsideredUnused(m.getExecutable(), model));
                }
                return true;
            }
            default -> throw new IllegalArgumentException("Unsupported element: " + element.getClass().getName());
        }
    }

    private void checkUnused(CtNamedElement ctElement, CodeModel model) {
        if (ctElement.isImplicit() || !ctElement.getPosition().isValidPosition()) {
            return;
        }

        boolean unused = isConsideredUnused(ctElement, model);

        if (unused) {
            ProblemType problemType = ProblemType.UNUSED_CODE_ELEMENT;
            if (ctElement instanceof CtModifiable ctModifiable && ctModifiable.isPrivate()) {
                problemType = ProblemType.UNUSED_CODE_ELEMENT_PRIVATE;
            }

            String name = ctElement.getSimpleName();

            // in spoon constructors are called <init>, which is not helpful
            if (ctElement instanceof CtConstructor<?> ctConstructor) {
                name = "%s()".formatted(ctConstructor.getDeclaringType().getSimpleName());
            }

            addLocalProblem(
                ctElement,
                new LocalizedMessage("unused-element", Map.of("name", name)),
                problemType
            );
        }
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        CodeModel model = staticAnalysis.getCodeModel();

        staticAnalysis.getModel().getRootPackage().accept(new CtScanner() {
            @Override
            public <T> void visitCtLocalVariable(CtLocalVariable<T> ctLocalVariable) {
                checkUnused(ctLocalVariable, model);
                super.visitCtLocalVariable(ctLocalVariable);
            }

            @Override
            public <T> void visitCtMethod(CtMethod<T> ctMethod) {
                if (MethodHierarchy.isOverridingMethod(ctMethod)
                    || MethodUtil.isMainMethod(ctMethod)) {
                    super.visitCtMethod(ctMethod);
                    return;
                }

                checkUnused(ctMethod, model);
                super.visitCtMethod(ctMethod);
            }

            @Override
            public <T> void visitCtConstructor(CtConstructor<T> ctConstructor) {
                if (ctConstructor.isPrivate()) {
                    super.visitCtConstructor(ctConstructor);
                    return;
                }

                checkUnused(ctConstructor, model);
                super.visitCtConstructor(ctConstructor);
            }

            @Override
            public <T> void visitCtParameter(CtParameter<T> ctParameter) {
                if (MethodUtil.isInOverridingMethod(ctParameter)
                    || MethodUtil.isInMainMethod(ctParameter)
                    || ctParameter.getParent() instanceof CtLambda<?>
                    || ctParameter.getParent(CtInterface.class) != null) {
                    super.visitCtParameter(ctParameter);
                    return;
                }

                checkUnused(ctParameter, model);

                super.visitCtParameter(ctParameter);
            }

            @Override
            public void visitCtTypeParameter(CtTypeParameter ctTypeParameter) {
                checkUnused(ctTypeParameter, model);
                super.visitCtTypeParameter(ctTypeParameter);
            }

            @Override
            public <T> void visitCtField(CtField<T> ctField) {
                if (ctField.getSimpleName().equals("serialVersionUID")) {
                    super.visitCtField(ctField);
                    return;
                }

                checkUnused(ctField, model);
                super.visitCtField(ctField);
            }

            // do not want to deal with false-positives for now
            /*
            @Override
            public <T> void visitCtClass(CtClass<T> ctType) {
                if (ctType.isAnonymous() || ctType.getMethods().stream().anyMatch(SpoonUtil::isMainMethod)) {
                    super.visitCtClass(ctType);
                    return;
                }

                checkUnused(staticAnalysis, ctType);
                super.visitCtClass(ctType);
            }

            @Override
            public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
                checkUnused(staticAnalysis, ctEnum);
                super.visitCtEnum(ctEnum);
            }*/
        });
    }

    @Override
    public Optional<Integer> maximumProblems() {
        return Optional.of(6);
    }
}
