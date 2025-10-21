package de.firemage.autograder.core.check.complexity;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.FactoryUtil;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StatementUtil;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.effects.AssignmentEffect;
import de.firemage.autograder.core.integrated.effects.Effect;
import de.firemage.autograder.core.integrated.evaluator.Evaluator;
import de.firemage.autograder.core.integrated.evaluator.algebra.ApplyAbsorptionLaw;
import de.firemage.autograder.core.integrated.evaluator.algebra.ApplyShortCircuit;
import de.firemage.autograder.core.integrated.evaluator.fold.EvaluateLiteralOperations;
import de.firemage.autograder.core.integrated.evaluator.fold.EvaluatePartialLiteralOperations;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ExecutableCheck(reportedProblems = { ProblemType.REDUNDANT_IF_FOR_BOOLEAN })
public class RedundantIfForBooleanCheck extends IntegratedCheck {
    private static final Set<String> METHODS_TO_IGNORE = Set.of("equals", "hashCode");

    private String makeSuggestion(CtExpression<?> ctExpression, Effect thenEffect, Effect elseEffect) {
        if (thenEffect instanceof AssignmentEffect thenAssignment && elseEffect instanceof AssignmentEffect) {
            return "%s = %s".formatted(
                thenAssignment.target(),
                ctExpression
            );
        }

        // otherwise it is a return statement
        return "return %s".formatted(ctExpression);
    }

    private void checkIfElse(CtExpression<?> condition, CtStatement thenStmt, CtStatement elseStmt) {
        Effect thenEffect = StatementUtil.tryMakeEffect(thenStmt).orElse(null);
        Effect elseEffect = StatementUtil.tryMakeEffect(elseStmt).orElse(null);

        // skip if they are not both return statements or both assignments to the same variable
        if (thenEffect == null
            || elseEffect == null
            || !thenEffect.isSameEffect(elseEffect)
            || !(thenStmt instanceof CtReturn<?>) && !(thenStmt instanceof CtAssignment<?, ?>)
            || thenEffect.value().isEmpty()
            || elseEffect.value().isEmpty()) {
            return;
        }

        CtExpression<?> thenValue = thenEffect.value().get();
        CtExpression<?> elseValue = elseEffect.value().get();

        // skip if they do not assign or return a boolean expression
        if (!ExpressionUtil.isBoolean(thenValue) || !ExpressionUtil.isBoolean(elseValue)) {
            return;
        }

        // skip duplicate values that are identical, because this would conflict with the DuplicateIfBlockCheck
        if (thenValue.equals(elseValue)) {
            return;
        }

        // skip if neither of the values is a literal
        if (!(thenValue instanceof CtLiteral<?>) && !(elseValue instanceof CtLiteral<?>)) {
            return;
        }

        // The code if (condition) { return thenValue; } else { return elseValue; } is equivalent to
        // return condition && thenValue || !condition && elseValue;
        //
        // Depending on the code, the suggestion can be simplified, for example a && true is equivalent to a.
        // The following code will apply these simplifications to the suggestion.

        Evaluator evaluator = new Evaluator(
            ApplyShortCircuit.create(),
            EvaluateLiteralOperations.create(),
            EvaluatePartialLiteralOperations.create(),
            ApplyAbsorptionLaw.create()
        );
        CtExpression<?> combinedCondition = evaluator.evaluate(FactoryUtil.createBinaryOperator(
            FactoryUtil.createBinaryOperator(condition, thenValue, BinaryOperatorKind.AND),
            FactoryUtil.createBinaryOperator(ExpressionUtil.negate(condition), elseValue, BinaryOperatorKind.AND),
            BinaryOperatorKind.OR
        ));

        addLocalProblem(
            condition,
            new LocalizedMessage(
                "common-reimplementation",
                Map.of(
                    "suggestion", makeSuggestion(combinedCondition, thenEffect, elseEffect)
                )
            ),
            ProblemType.REDUNDANT_IF_FOR_BOOLEAN
        );
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtBlock<?>>() {
            @Override
            public void process(CtBlock<?> block) {
                CtMethod<?> parentMethod = block.getParent(CtMethod.class);
                if (parentMethod != null && METHODS_TO_IGNORE.contains(parentMethod.getSimpleName())) {
                    return;
                }

                List<CtStatement> statements = StatementUtil.getEffectiveStatements(block);

                for (int i = 0; i < statements.size(); i++) {
                    CtStatement statement = statements.get(i);

                    if (!(statement instanceof CtIf ifStmt) || ifStmt.getThenStatement() == null) {
                        continue;
                    }

                    CtStatement thenStatement = StatementUtil.unwrapStatement(ifStmt.getThenStatement());
                    CtStatement elseStatement = ifStmt.getElseStatement();

                    // if(...) { return true } return false
                    if (elseStatement == null && i + 1 < statements.size()) {
                        elseStatement = statements.get(i + 1);
                    }

                    if (elseStatement == null) {
                        continue;
                    }

                    checkIfElse(
                        ifStmt.getCondition(),
                        thenStatement,
                        StatementUtil.unwrapStatement(elseStatement)
                    );
                }
            }
        });
    }
}
