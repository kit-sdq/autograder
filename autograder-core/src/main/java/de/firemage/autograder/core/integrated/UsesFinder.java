package de.firemage.autograder.core.integrated;

import spoon.processing.FactoryAccessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTypePattern;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Provides usage-relationships between code elements in the code model.
 * Uses are tracked for variables of all kinds, type parameters, executables, and types.
 * The graph is built once at construction time with a single pass over the model for all elements,
 * so that all subsequent queries are fast.
 */
public class UsesFinder {
    private static final String METADATA_KEY = "autograder_uses";

    private final UsesScanner scanner;

    private UsesFinder(CtModel model) {
        this.scanner = new UsesScanner();
        model.getRootPackage().accept(this.scanner);
    }

    public static void buildFor(CtModel model) {
        UsesFinder uses = new UsesFinder(model);
        model.getRootPackage().putMetadata(METADATA_KEY, uses);
    }

    private static UsesFinder getFor(FactoryAccessor factoryAccessor) {
        var uses = (UsesFinder) ElementUtil.getRootPackage(factoryAccessor).getMetadata(METADATA_KEY);
        if (uses == null) {
            throw new IllegalArgumentException("No uses information available for this model");
        }
        return uses;
    }

    /**
     * Find all uses of the given element within the model.
     * This is an (untyped) alternative to the more specific methods (variableUses, typeParameterUses, executableUses, typeUses).
     * @param element
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static CtElementStream<CtElement> getAllUses(CtNamedElement element) {
        return switch (element) {
            case CtVariable variable -> UsesFinder.variableUses(variable).asUntypedStream();
            case CtTypeParameter typeParameter -> UsesFinder.typeParameterUses(typeParameter).asUntypedStream();
            case CtExecutable executable -> UsesFinder.executableUses(executable).asUntypedStream();
            case CtType type -> UsesFinder.typeUses(type).asUntypedStream();
            default -> throw new IllegalArgumentException("Unsupported element: " + element.getClass().getName());
        };
    }

    public static CtElementStream<CtVariableAccess<?>> variableUses(CtVariable<?> variable) {
        return CtElementStream.of(UsesFinder.getFor(variable).scanner.variableUses.getOrDefault(variable, Set.of())).assumeElementType();
    }

    @SuppressWarnings("unchecked")
    public static CtElementStream<CtVariableWrite<?>> variableWrites(CtVariable<?> variable) {
        return (CtElementStream<CtVariableWrite<?>>) (Object) CtElementStream.of(UsesFinder.getFor(variable).scanner.variableUses.getOrDefault(variable, Set.of())).assumeElementType().ofType(CtVariableWrite.class);
    }

    @SuppressWarnings("unchecked")
    public static CtElementStream<CtVariableRead<?>> variableReads(CtVariable<?> variable) {
        return (CtElementStream<CtVariableRead<?>>) (Object) CtElementStream.of(UsesFinder.getFor(variable).scanner.variableUses.getOrDefault(variable, Set.of())).assumeElementType().ofType(CtVariableRead.class);
    }

    public static CtElementStream<CtTypeParameterReference> typeParameterUses(CtTypeParameter typeParameter) {
        return CtElementStream.of(UsesFinder.getFor(typeParameter).scanner.typeParameterUses.getOrDefault(typeParameter, List.of()));
    }

    public static CtElementStream<CtElement> executableUses(CtExecutable<?> executable) {
        return CtElementStream.of(UsesFinder.getFor(executable).scanner.executableUses.getOrDefault(executable, List.of()));
    }

    public static CtElementStream<CtTypeReference<?>> typeUses(CtType<?> type) {
        return CtElementStream.of(UsesFinder.getFor(type).scanner.typeUses.getOrDefault(type, List.of())).assumeElementType();
    }

    public static boolean isSubtypeOf(CtType<?> potentialSubtype, CtType<?> parentType) {
        if (parentType == potentialSubtype
            || potentialSubtype.isPrimitive()
            || parentType.isPrimitive()) {
            return true;
        }

        Set<CtType> knownSubtypes = UsesFinder.getFor(potentialSubtype).scanner.subtypes.getOrDefault(parentType, new LinkedHashSet<>());

        // all types that are not shadow types, should be present
        // in the source code and therefore in the set of known subtypes
        if (!potentialSubtype.isShadow()) {
            return knownSubtypes.contains(potentialSubtype);
        }

        // for shadow types we can't rely on the set of known subtypes (they might be incomplete)
        //
        // but if the potential subtype is already known to be a subtype of the parent type,
        // we can return true immediately
        if (knownSubtypes.contains(potentialSubtype)) {
            return true;
        }

        boolean result = TypeUtil.streamAllSuperTypes(potentialSubtype).anyMatch(type -> parentType == type);

        // this is just a sanity check to ensure that our implementation is correct
        if (CoreUtil.isInDebugMode() && result != potentialSubtype.isSubtypeOf(parentType.getReference())) {
            throw new IllegalStateException("Inconsistent subtype information for %s and %s".formatted(
                potentialSubtype.getQualifiedName(),
                parentType.getQualifiedName()
            ));
        }

        // keep track of the result for future queries
        if (result) {
            knownSubtypes.add(potentialSubtype);
        }

        return result;
    }

    /**
     * Returns the subtypes in the model of the given type.
     *
     * @param type the type should be declared in the source code and not a shadow type like java.util.List, for these types the result will be empty
     * @param includeSelf whether the given type should be included in the result
     * @return a stream of subtypes of the given type
     */
    public static CtElementStream<CtType<?>> subtypesOf(CtType<?> type, boolean includeSelf) {
        Stream<CtType<?>> selfStream = includeSelf ? Stream.of(type) : Stream.empty();
        return CtElementStream.concat(
            selfStream,
            CtElementStream.of(UsesFinder.getFor(type).scanner.subtypes.getOrDefault(type, new LinkedHashSet<>())).assumeElementType()
        ).filter(ctType -> !ctType.isShadow());
    }

    // It is difficult to determine whether a variable is being accessed by the variable access,
    // because references to different variables might be equal or the CtVariable can not be obtained
    // from the CtVariableAccess.
    //
    // This class keeps track of all variable accesses and their corresponding variables, which is why
    // this method exists here.
    public static boolean isAccessingVariable(CtVariable<?> ctVariable, CtVariableAccess<?> ctVariableAccess) {
        return UsesFinder.getDeclaredVariable(ctVariableAccess) == ctVariable;
    }

    public static CtVariable<?> getDeclaredVariable(CtVariableAccess<?> ctVariableAccess) {
        return UsesFinder.getFor(ctVariableAccess).scanner.variableAccessDeclarations.getOrDefault(ctVariableAccess, null);
    }

    static CtExecutable<?> getExecutableDeclaration(CtExecutableReference<?> ctExecutableReference) {
        return UsesFinder.getFor(ctExecutableReference).scanner.executableDeclarations.computeIfAbsent(ctExecutableReference, CtExecutableReference::getExecutableDeclaration);
    }

    /**
     * The scanner searches for uses of supported code elements in a single pass over the entire model.
     * Since inserting into the hash map requires (amortized) constant time, usage search runs in O(n) time.
     */
    @SuppressWarnings("rawtypes")
    private static class UsesScanner extends CtScanner {
        // The IdentityHashMaps are very important here, since
        // E.g. CtVariable's equals method considers locals with the same name to be equal
        private final Map<CtVariable, Set<CtVariableAccess>> variableUses = new IdentityHashMap<>();
        private final Map<CtVariableAccess, CtVariable> variableAccessDeclarations = new IdentityHashMap<>();
        private final Map<CtTypeParameter, List<CtTypeParameterReference>> typeParameterUses = new IdentityHashMap<>();
        private final Map<CtExecutable, List<CtElement>> executableUses = new IdentityHashMap<>();
        private final Map<CtType, List<CtTypeReference>> typeUses = new IdentityHashMap<>();
        private final Map<CtType, SequencedSet<CtType>> subtypes = new IdentityHashMap<>();
        private final Map<CtExecutableReference, CtExecutable> executableDeclarations = new IdentityHashMap<>();

        // Caches the current instanceof pattern variables, since Spoon doesn't track them yet
        // We are conservative: A pattern introduces a variable until the end of the current block
        // (in reality, the scope is based on 'normal completion', see JLS, but the rules are way too complex for us)
        private final Deque<Map<String, CtVariable>> instanceofPatternVariables = new ArrayDeque<>();

        @Override
        public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
            this.recordVariableAccess(variableRead);
            super.visitCtVariableRead(variableRead);
        }

        @Override
        public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite) {
            this.recordVariableAccess(variableWrite);
            super.visitCtVariableWrite(variableWrite);
        }

        @Override
        public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
            this.recordVariableAccess(fieldRead);
            super.visitCtFieldRead(fieldRead);
        }

        @Override
        public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
            this.recordVariableAccess(fieldWrite);
            super.visitCtFieldWrite(fieldWrite);
        }

        @Override
        public <T> void visitCtInvocation(CtInvocation<T> invocation) {
            if (invocation.getTarget() instanceof CtVariableAccess reference) {
                this.recordVariableAccess(reference);
            }
            this.recordExecutableReference(invocation.getExecutable(), invocation);
            super.visitCtInvocation(invocation);
        }

        @Override
        public void visitCtTypePattern(CtTypePattern pattern) {
            var variable = pattern.getVariable();
            this.instanceofPatternVariables.peek().put(variable.getSimpleName(), variable);
            super.visitCtTypePattern(pattern);
        }

        @Override
        public void visitCtTypeParameterReference(CtTypeParameterReference reference) {
            this.recordTypeParameterReference(reference);
            super.visitCtTypeParameterReference(reference);
        }

        @Override
        public <T, E extends CtExpression<?>> void visitCtExecutableReferenceExpression(CtExecutableReferenceExpression<T, E> expression) {
            this.recordExecutableReference(expression.getExecutable(), expression);
            super.visitCtExecutableReferenceExpression(expression);
        }

        @Override
        public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
            this.recordExecutableReference(ctConstructorCall.getExecutable(), ctConstructorCall);
            super.visitCtConstructorCall(ctConstructorCall);
        }

        @Override
        public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
            this.recordTypeReference(reference);
            super.visitCtTypeReference(reference);
        }

        @Override
        public <R> void visitCtBlock(CtBlock<R> block) {
            this.instanceofPatternVariables.push(new HashMap<>());
            super.visitCtBlock(block);
            this.instanceofPatternVariables.pop();
        }

        @Override
        public <T> void visitCtClass(CtClass<T> ctClass) {
            // CtType:
            // - CtClass
            // - CtEnum
            // - CtInterface
            // - CtRecord
            // - CtTypeParameter
            //
            // CtClass:
            // - CtEnum
            // - CtRecord
            this.recordCtType(ctClass);
            super.visitCtClass(ctClass);
        }

        @Override
        public <T> void visitCtInterface(CtInterface<T> ctInterface) {
            this.recordCtType(ctInterface);
            super.visitCtInterface(ctInterface);
        }

        private void recordVariableAccess(CtVariableAccess<?> variableAccess) {
            var variable = variableAccess.getVariable().getDeclaration();

            if (variable == null) {
                // Try a few other ways to get the declaration
                if (variableAccess.getVariable() instanceof CtLocalVariableReference) {
                    // Look for an instanceof pattern variable which is in scope
                    for (var scope : this.instanceofPatternVariables) {
                        variable = scope.get(variableAccess.getVariable().getSimpleName());
                        if (variable != null) {
                            break;
                        }
                    }
                } else if (variableAccess.getVariable() instanceof CtFieldReference<?> fieldReference) {
                    // We can use the shadow model
                    variable = fieldReference.getFieldDeclaration();
                }
            }

            if (variable != null) {
                var accesses = this.variableUses.computeIfAbsent(variable, k -> Collections.newSetFromMap(new IdentityHashMap<>()));
                accesses.add(variableAccess);
                this.variableAccessDeclarations.put(variableAccess, variable);
            }
        }

        private void recordTypeParameterReference(CtTypeParameterReference reference) {
            CtTypeParameter parameter = reference.getDeclaration();
            if (parameter != null) {
                var uses = this.typeParameterUses.computeIfAbsent(parameter, k -> new ArrayList<>());
                uses.add(reference);
            }
        }

        private void recordExecutableReference(CtExecutableReference reference, CtElement referencingElement) {
            var executable = reference.getExecutableDeclaration();
            if (executable != null) {
                this.executableDeclarations.put(reference, executable);
                var uses = this.executableUses.computeIfAbsent(executable, k -> new ArrayList<>());
                uses.add(referencingElement);
            }
        }

        private void recordTypeReference(CtTypeReference reference) {
            if (reference instanceof CtArrayTypeReference<?> arrayType) {
                reference = arrayType.getArrayType();
            }

            var type = reference.getTypeDeclaration();
            if (type != null) {
                var uses = this.typeUses.computeIfAbsent(type, k -> new ArrayList<>());
                uses.add(reference);
            }
        }

        private void recordCtType(CtType<?> ctType) {
            for (CtType<?> superType : TypeUtil.allSuperTypes(ctType)) {
                this.subtypes.computeIfAbsent(superType, key -> new LinkedHashSet<>()).add(ctType);
            }
        }
    }
}
