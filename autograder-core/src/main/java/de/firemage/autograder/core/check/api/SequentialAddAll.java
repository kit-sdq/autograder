package de.firemage.autograder.core.check.api;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.check.api.UseEnumValues.CtEnumFieldRead;
import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.MethodUtil;
import de.firemage.autograder.core.integrated.StatementUtil;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.TypeUtil;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtVariableReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ExecutableCheck(reportedProblems = {
    ProblemType.SEQUENTIAL_ADD_ALL,
    ProblemType.USE_ENUM_VALUES,
})
public class SequentialAddAll extends IntegratedCheck {
    private static final int MIN_ADD_ALL_SIZE = 4;

    private record AddInvocation(
        CtVariableReference<?> collection,
        CtExecutableReference<?> executableReference,
        CtExpression<?> value
    ) {
        static Optional<AddInvocation> of(CtStatement ctStatement) {
            CtType<?> collectionType = ctStatement.getFactory().Type().get(java.util.Collection.class);
            if (!(ctStatement instanceof CtInvocation<?> ctInvocation)
                || !(ctInvocation.getTarget() instanceof CtVariableAccess<?> ctVariableAccess)
                || ctVariableAccess.getVariable().getType() instanceof CtTypeParameterReference
                || !ctVariableAccess.getVariable().getType().isSubtypeOf(collectionType.getReference())) {
                return Optional.empty();
            }

            CtExecutableReference<?> executableReference = ctInvocation.getExecutable();
            CtVariableReference<?> collection = ctVariableAccess.getVariable();
            if (!MethodUtil.isSignatureEqualTo(
                executableReference,
                boolean.class,
                "add",
                Object.class)) {
                return Optional.empty();
            }

            return Optional.of(new AddInvocation(collection, executableReference, ctInvocation.getArguments().get(0)));
        }
    }

    private void reportProblem(CtVariableReference<?> ctVariable, List<? extends CtExpression<?>> values) {
        // check if the given values are enum values and find the enum they belong to
        Collection<CtEnumValue<?>> fieldReads = new ArrayList<>();
        CtEnum<?> ctEnum = null;
        for (CtExpression<?> value : values) {
            CtEnumFieldRead fieldRead = CtEnumFieldRead.of(value).orElse(null);
            if (fieldRead == null) {
                ctEnum = null;
                break;
            }

            if (ctEnum == null) {
                ctEnum = fieldRead.ctEnum();
            }

            if (!ctEnum.equals(fieldRead.ctEnum())) {
                ctEnum = null;
                break;
            }

            fieldReads.add(fieldRead.ctEnumValue());
        }

        if (ctEnum != null && UseEnumValues.checkEnumValues(ctEnum, TypeUtil.isOrderedCollection(ctVariable.getType()), fieldReads)) {
            addLocalProblem(
                values.getFirst().getParent(CtStatement.class),
                new LocalizedMessage(
                    "common-reimplementation",
                    Map.of(
                        "suggestion", "%s.addAll(Arrays.asList(%s.values()))".formatted(
                            ctVariable.getSimpleName(),
                            ctEnum.getSimpleName()
                        )
                    )
                ),
                ProblemType.USE_ENUM_VALUES
            );
            return;
        }

        if (values.size() < MIN_ADD_ALL_SIZE) {
            return;
        }

        // The added values are not enum values, but some other value.
        //
        // Suggesting to use `addAll` instead of multiple `add` invocations is pedantic,
        // when the values can not be refactored into a constant:
        //
        // ```
        // list.addAll(List.of(
        //     var1,
        //     var2,
        //     var3
        // ));
        // ```
        // is not much better than
        // ```
        // list.add(var1);
        // list.add(var2);
        // list.add(var3);
        // ```
        //
        // Therefore, we only suggest to use `addAll` if the values can be refactored into a constant.

        for (CtExpression<?> value: values) {
            if (!ExpressionUtil.isConstantExpressionOr(value, e -> false)) {
                return;
            }
        }

        addLocalProblem(
            values.getFirst().getParent(CtStatement.class),
            new LocalizedMessage(
                "common-reimplementation",
                Map.of(
                    "suggestion", "private static final List<%s> SOME_GOOD_NAME = List.of(%s); /* ... */ %s.addAll(SOME_GOOD_NAME)".formatted(
                        ExpressionUtil.getExpressionType(values.getFirst()),
                        values.stream()
                            .map(CtElement::toString)
                            .collect(Collectors.joining(", ")),
                        ctVariable.getSimpleName()
                    )
                )
            ),
            ProblemType.SEQUENTIAL_ADD_ALL
        );
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtBlock<?>>() {
            @Override
            public void process(CtBlock<?> ctBlock) {
                if (ctBlock.isImplicit() || !ctBlock.getPosition().isValidPosition() || ctBlock.getStatements().size() < MIN_ADD_ALL_SIZE) {
                    return;
                }

                CtVariableReference<?> collection = null;
                List<CtExpression<?>> addedValues = new ArrayList<>();

                // We want to find a sequence of add invocations in the block like this:
                //
                // ```
                // list.add("a");
                // list.add("b");
                // list.add("c");
                // list.add("d");
                // ...
                // ```
                for (CtStatement ctStatement : StatementUtil.getEffectiveStatements(ctBlock)) {
                    AddInvocation addInvocation = AddInvocation.of(ctStatement).orElse(null);
                    // the current statement might not be an add invocation
                    if (addInvocation == null) {
                        // This if handles the case where there is code after the sequence of add invocations:
                        //
                        // ```
                        // list.add("a");
                        // list.add("b");
                        // list.add("c");
                        // list.add("d");
                        //
                        // System.out.println("Hello, World!"); // <- extra code
                        // ```
                        if (addedValues.size() > 1 && collection != null) {
                            reportProblem(collection, addedValues);
                        }

                        // otherwise reset the state and start over
                        collection = null;
                        continue;
                    }

                    // if the collection is not yet known, we have found the first add invocation
                    if (collection == null) {
                        collection = addInvocation.collection();
                        addedValues.clear();
                    }

                    // we might encounter an add invocation for a different collection
                    if (!collection.equals(addInvocation.collection())) {
                        // if we have enough values, report the problem
                        if (addedValues.size() > 1) {
                            reportProblem(collection, addedValues);
                        }

                        // if there are multiple collections, just invalidate the state
                        collection = null;
                        continue;
                    }

                    addedValues.add(addInvocation.value());
                }

                // this happens when the last statement in the block is an add invocation
                if (addedValues.size() > 1 && collection != null) {
                    reportProblem(collection, addedValues);
                }
            }
        });
    }
}
