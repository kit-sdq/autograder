package de.firemage.autograder.core.check.oop;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.MethodUtil;
import de.firemage.autograder.core.integrated.TypeUtil;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExecutableCheck(reportedProblems = { ProblemType.CONCRETE_COLLECTION_AS_FIELD_OR_RETURN_VALUE })
public class ConcreteCollectionCheck extends IntegratedCheck {
    private static final List<Class<?>> ALLOWED_TYPES = List.of(
        java.util.Properties.class,
        java.util.LinkedHashSet.class,
        java.util.LinkedHashMap.class,
        java.util.EnumMap.class,
        java.util.EnumSet.class
    );

    private boolean isConcreteCollectionType(CtTypeReference<?> ctType) {
        return !ctType.isInterface() && Stream.of(java.util.Collection.class, java.util.Map.class)
            .anyMatch(ctClass -> TypeUtil.isSubtypeOf(ctType, ctClass));
    }

    private boolean isAllowedType(CtTypeReference<?> ctTypeReference) {
        return TypeUtil.isTypeEqualTo(ctTypeReference, ALLOWED_TYPES);
    }

    private boolean isInAllowedContext(CtTypeReference<?> ctTypeReference) {
        // new ArrayList<>()
        CtConstructorCall<?> ctConstructorCall = ctTypeReference.getParent(CtConstructorCall.class);
        if (ctConstructorCall != null && ctTypeReference.equals(ctConstructorCall.getType())) {
            return true;
        }

        // ArrayList::new
        if (ctTypeReference.getParent(CtExecutableReferenceExpression.class) != null
            || ctTypeReference.getParent(CtExecutableReference.class) != null) {
            return true;
        }

        // calling (static) method
        CtInvocation<?> ctInvocation = ctTypeReference.getParent(CtInvocation.class);
        if (ctInvocation != null
            && ctInvocation.getTarget() != null
            && ctInvocation.getTarget().equals(ctTypeReference.getParent(CtTypeAccess.class))) {
            return true;
        }

        // new ArrayList[10]
        if (ctTypeReference.getParent(CtNewArray.class) != null) {
            return true;
        }

        // (ArrayList<?>) myList
        CtVariable<?> ctVariable = ctTypeReference.getParent(CtVariable.class);
        if (ctVariable != null
            && ctVariable.getDefaultExpression() != null
            && ctVariable.getDefaultExpression().getTypeCasts().contains(ctTypeReference)) {
            return true;
        }

        CtVariableRead<?> ctVariableRead = ctTypeReference.getParent(CtVariableRead.class);
        if (ctVariableRead != null && ctVariableRead.getTypeCasts().contains(ctTypeReference)) {
            return true;
        }

        // Array access
        if (ctTypeReference.getParent(CtArrayTypeReference.class) != null &&
                (ctTypeReference.getParent(CtVariableAccess.class) != null
                        || ctTypeReference.getParent(CtArrayAccess.class) != null
                        || ctTypeReference.getParent(CtFieldAccess.class) != null)) {
            return true;
        }

        // ArrayList.class
        CtFieldRead<?> ctFieldRead = ctTypeReference.getParent(CtFieldRead.class);
        if (ctFieldRead != null) {
            CtFieldReference<?> ctFieldReference = ctFieldRead.getVariable();
            return ctFieldReference.getDeclaringType().equals(ctTypeReference)
                && TypeUtil.isTypeEqualTo(ctFieldReference.getType(), java.lang.Class.class);
        }

        // AbstractMap.SimpleEntry
        CtTypeReference<?> parentTypeReference = ctTypeReference.getParent(CtTypeReference.class);
        if (parentTypeReference != null) {
            return ctTypeReference.equals(parentTypeReference.getDeclaringType());
        }

        // variable instanceof ArrayList<?>
        CtBinaryOperator<?> ctBinaryOperator = ctTypeReference.getParent(CtBinaryOperator.class);
        if (ctBinaryOperator != null) {
            return ctBinaryOperator.getKind() == BinaryOperatorKind.INSTANCEOF;
        }

        // MyClass<T> extends ArrayList<T>
        CtElement parent = ctTypeReference.getParent();
        if (parent == null) {
            return false;
        }

        if (parent instanceof CtClass<?> parentType && parentType.getSuperclass() != null) {
            return parentType.getSuperclass().equals(ctTypeReference);
        }

        // MyList.this.method() and MyList.super.method()
        // for MyList extends ArrayList implements List
        if (ctTypeReference.getParent(CtSuperAccess.class) != null || ctTypeReference.getParent(CtThisAccess.class) != null) {
            return true;
        }

        return false;
    }

    private String formatTypeName(CtTypeReference<?> ctTypeReference) {
        if (ctTypeReference == null) {
            return "";
        }

        String baseName = ctTypeReference.getSimpleName();
        CtTypeReference<?> declaringType = ctTypeReference.getDeclaringType();
        if (declaringType != null) {
            baseName = formatTypeName(declaringType) + "." + baseName;
        }

        List<CtTypeReference<?>> typeArguments = ctTypeReference.getActualTypeArguments();
        if (typeArguments.isEmpty()) {
            return baseName;
        }

        String formattedArgs = typeArguments.stream()
            .map(this::formatTypeName)
            .collect(Collectors.joining(", "));

        return baseName + "<" + formattedArgs + ">";
    }

    private boolean checkCtTypeReference(CtTypeReference<?> ctTypeReference) {
        if (!ctTypeReference.getPosition().isValidPosition()
            // arrays are special, they will be handled by the code
            && (ctTypeReference.getParent(CtArrayTypeReference.class) == null)) {
            return true;
        }

        if (MethodUtil.isInOverridingMethod(ctTypeReference)
            || this.isInAllowedContext(ctTypeReference)
            || this.isAllowedType(ctTypeReference)) {
            return false;
        }

        if (!this.isConcreteCollectionType(ctTypeReference)) {
            if (ctTypeReference instanceof CtArrayTypeReference<?> ctArrayTypeReference
                && this.checkCtTypeReference(ctArrayTypeReference.getArrayType())) {
                return true;
            }

            // check nested types:
            for (CtTypeReference<?> typeArgument : ctTypeReference.getActualTypeArguments()) {
                if (this.checkCtTypeReference(typeArgument)) {
                    return true;
                }
            }

            return false;
        }

        CtElement element = ctTypeReference;
        while (!element.getPosition().isValidPosition()
            && (element.getParent(CtArrayTypeReference.class) != null)) {
            element = element.getParent(CtArrayTypeReference.class);
        }

        this.addLocalProblem(
            element,
            new LocalizedMessage(
                "concrete-collection",
                Map.of(
                    "type", this.formatTypeName(ctTypeReference)
                )
            ),
            ProblemType.CONCRETE_COLLECTION_AS_FIELD_OR_RETURN_VALUE
        );

        return true;
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        // Checks for fields, parameters and return types
        staticAnalysis.getModel().getRootPackage().accept(new CtScanner() {
            @Override
            public <T> void visitCtMethod(CtMethod<T> ctMethod) {
                if (ctMethod.isImplicit() || !ctMethod.getPosition().isValidPosition()) {
                    super.visitCtMethod(ctMethod);
                    return;
                }

                boolean hasError = checkCtTypeReference(ctMethod.getType());

                if (hasError) {
                    // skip scanning the TYPE to avoid duplicate errors
                    this.enter(ctMethod);
                    this.scan(CtRole.ANNOTATION, ctMethod.getAnnotations());
                    this.scan(CtRole.TYPE_PARAMETER, ctMethod.getFormalCtTypeParameters());
                    // scan(CtRole.TYPE, ctMethod.getType());
                    this.scan(CtRole.PARAMETER, ctMethod.getParameters());
                    this.scan(CtRole.THROWN, ctMethod.getThrownTypes());
                    this.scan(CtRole.BODY, ctMethod.getBody());
                    this.scan(CtRole.COMMENT, ctMethod.getComments());
                    this.exit(ctMethod);

                    return;
                }

                super.visitCtMethod(ctMethod);
            }

            @Override
            public <T> void visitCtField(CtField<T> ctVariable) {
                boolean hasError = checkCtTypeReference(ctVariable.getType());
                if (!hasError) {
                    super.visitCtField(ctVariable);
                }
            }

            @Override
            public <T> void visitCtLocalVariable(CtLocalVariable<T> ctVariable) {
                if (ctVariable.isInferred()) {
                    super.visitCtLocalVariable(ctVariable);
                    return;
                }

                boolean hasError = checkCtTypeReference(ctVariable.getType());
                if (!hasError) {
                    super.visitCtLocalVariable(ctVariable);
                }
            }

            @Override
            public <T> void visitCtParameter(CtParameter<T> ctVariable) {
                if (ctVariable.isImplicit() || !ctVariable.getPosition().isValidPosition()) {
                    super.visitCtParameter(ctVariable);
                    return;
                }

                boolean hasError = checkCtTypeReference(ctVariable.getType());
                if (!hasError) {
                    super.visitCtParameter(ctVariable);
                }
            }

            @Override
            public void visitCtRecord(CtRecord ctRecord) {
                this.enter(ctRecord);
                this.scan(CtRole.ANNOTATION, ctRecord.getAnnotations());
                this.scan(CtRole.INTERFACE, ctRecord.getSuperInterfaces());
                this.scan(CtRole.TYPE_PARAMETER, ctRecord.getFormalCtTypeParameters());
                ctRecord.getFields().forEach(this::visitCtField);
                this.scan(CtRole.COMMENT, ctRecord.getComments());
                this.exit(ctRecord);
            }
        });
    }

    @Override
    public Optional<Integer> maximumProblems() {
        return Optional.of(5);
    }
}
