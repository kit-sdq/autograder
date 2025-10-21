package de.firemage.autograder.core.integrated.evaluator;

import de.firemage.autograder.core.integrated.ExpressionUtil;
import de.firemage.autograder.core.integrated.UsesFinder;
import de.firemage.autograder.core.integrated.evaluator.fold.ApplyCasts;
import de.firemage.autograder.core.integrated.evaluator.fold.ChainedFold;
import de.firemage.autograder.core.integrated.evaluator.fold.DeduplicateOperatorApplication;
import de.firemage.autograder.core.integrated.evaluator.fold.EvaluateLiteralOperations;
import de.firemage.autograder.core.integrated.evaluator.fold.EvaluatePartialLiteralOperations;
import de.firemage.autograder.core.integrated.evaluator.fold.Fold;
import de.firemage.autograder.core.integrated.evaluator.fold.RemoveRedundantCasts;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.eval.PartialEvaluator;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.DefaultImportComparator;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.ForceImportProcessor;
import spoon.reflect.visitor.ImportCleaner;
import spoon.reflect.visitor.ImportConflictDetector;
import spoon.support.compiler.VirtualFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestEvaluator {
    @SuppressWarnings("unchecked")
    private static <T> CtExpression<T> createExpression(String expression, String arguments) {
        Launcher launcher = new Launcher();

        if (arguments == null) {
            arguments = "";
        }

        launcher.addInputResource(new VirtualFile(
            "public class Test { void t(%s) { System.out.println(%s); } }".formatted(
                arguments,
                expression
            )
        ));

        Environment environment = launcher.getEnvironment();
        environment.setPrettyPrinterCreator(() -> new DefaultJavaPrettyPrinter(environment) {
            {
                // copy-pasted from StandardEnvironment#createPrettyPrinterAutoImport
                List<Processor<CtElement>> preprocessors = List.of(
                    // try to import as many types as possible
                    new ForceImportProcessor(),
                    // remove unused imports first. Do not add new imports at a time when conflicts are not resolved
                    new ImportCleaner().setCanAddImports(false),
                    // solve conflicts, the current imports are relevant too
                    new ImportConflictDetector(),
                    // compute final imports
                    new ImportCleaner().setImportComparator(new DefaultImportComparator())
                );
                this.setIgnoreImplicit(false);
                this.setPreprocessors(preprocessors);
                this.setMinimizeRoundBrackets(true);
            }
        });
        environment.setIgnoreSyntaxErrors(false);

        CtModel ctModel = launcher.buildModel();
        UsesFinder.buildFor(ctModel);

        CtMethod<?> ctMethod = new ArrayList<>(ctModel.getAllTypes()).get(0).getMethodsByName("t").get(0);
        CtAbstractInvocation<?> ctInvocation = (CtInvocation<?>) ctMethod.getBody().getStatements().get(0);

        return (CtExpression<T>) ctInvocation.getArguments().get(0);
    }

    private static void runExpressionTest(String expression, String arguments, String expected, List<Fold> folds) {
        PartialEvaluator evaluator = new Evaluator(ChainedFold.chain(folds));

        CtExpression<?> ctExpression = createExpression(expression, arguments);
        // the type should not change throughout the evaluation
        CtTypeReference<?> currentType = ExpressionUtil.getExpressionType(ctExpression).clone();
        CtExpression<?> result = evaluator.evaluate(ctExpression);

        assertEquals(expected, result.toString());
        assertEquals(currentType, ExpressionUtil.getExpressionType(result));
    }

    /**
     * Casts should only be applied to literals that are evaluated.
     * <p>
     * It is undesirable to, for example, change a char literal to an int literal.
     */
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression          | Arguments    | Expected             ",
            "a + 1 + (byte) 3     | int a        | a + 1 + (byte) 3     ",
            "a + 'b'              | byte a       | a + 'b'              ",
            "a + (short) 0        | byte a       | (int) a              ",
            "a + ('b' + 0)        | byte a       | a + 98               ",
        }
    )
    void testOnlyApplyLiteralCastsWhenNecessary(String expression, String arguments, String expected) {
        runExpressionTest(expression, arguments, expected, List.of(
            EvaluatePartialLiteralOperations.create(),
            EvaluateLiteralOperations.create()
        ));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression          | Arguments    | Expected             ",
            "a + 1 + (byte) 3     | int a        | a + 1 + 3            ",
        }
    )
    void testApplyCastsOnLiterals(String expression, String arguments, String expected) {
        runExpressionTest(expression, arguments, expected, List.of(ApplyCasts.onLiterals()));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression          | Arguments    | Expected             ",
            "-(-(a))              | int a        | a                    ",
            "-(-((short) a))      | int a        | (int) (short) a      ",
            "-(-(a))              | short a      | (int) a              ",
            "!(!a)                | boolean a    | a                    ",
        }
    )
    void testDeduplicateOperatorApplication(String expression, String arguments, String expected) {
        runExpressionTest(expression, arguments, expected, List.of(
            DeduplicateOperatorApplication.create()
        ));
    }


    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression          | Arguments    | Expected             ",
            "(float) 'a' + 1      |              | 98.0F                ",
            // contract: type casts are not ignored during partial evaluation
            "(byte) 400 + 20      |              | -92                  ",
        }
    )
    void testEvaluateLiteralOperation(String expression, String arguments, String expected) {
        runExpressionTest(expression, arguments, expected, List.of(
            EvaluateLiteralOperations.create()
        ));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = ';',
        useHeadersInDisplayName = true,
        value = {
            " Expression          ; Arguments    ; Expected             ",
            " true && b           ; boolean b    ; b                    ",
            " false && b          ; boolean b    ; false                ",
            " true || b           ; boolean b    ; true                 ",
            " false || b          ; boolean b    ; b                    ",
            " false == b          ; boolean b    ; false == b           ",
            " b == true           ; boolean b    ; b                    ",
            " a / 1               ; int a        ; a                    ",
            " a / 1.1             ; int a        ; a / 1.1              ",
            " a / 1.0             ; int a        ; (double) a           ",
            " 1 / a               ; int a        ; 1 / a                ",
            " b + 1               ; byte b       ; b + 1                ",
            " b + 0               ; byte b       ; (int) b              ",
            " f + 0.001           ; float f      ; f + 0.001            ",
            " a * 1               ; int a        ; a                    ",
            " a * 1.1             ; int a        ; a * 1.1              ",
            " a * 1.0             ; int a        ; (double) a           ",
            " 0 * a               ; int a        ; 0                    ",
            " 0.0 * a             ; int a        ; 0.0                  ",
            " b - 1               ; byte b       ; b - 1                ",
            " b - 0               ; byte b       ; (int) b              ",
            " f - 0.001           ; float f      ; f - 0.001            ",
            " f - 0.0             ; float f      ; (double) f           ",
            " 0 - b               ; byte b       ; 0 - b                ",
            " 0 - b - 0           ; byte b       ; 0 - b                ",
            " f - 0 - 0           ; float f      ; f                    ",
            " 0.0 - f             ; float f      ; 0.0 - f              ",
        }
    )
    void testEvaluatePartialLiteralOperation(String expression, String arguments, String expected) {
        runExpressionTest(expression, arguments, expected, List.of(
            EvaluatePartialLiteralOperations.create()
        ));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Expression                     | Arguments    | Expected              ",
            // redundant cast to the same type
            " (byte) b                       | byte b       | b                     ",
            // redundant cast to a boxed type (technically redundant, but would not preserve the type)
            " (Byte) b                       | byte b       | (Byte) b              ",
            // widening a type, then narrowing it is redundant
            " (char) (int) c                 | char c       | c                     ",
            " (char) (long) b                | byte b       | (char) b              ",
            " (int) (long) f                 | float f      | (int) f               ",
            " (int) (char) c                 | char c       | (int) c               ",
            " (Integer) (int) s              | short s      | (Integer) (int) s     ",
            " (int) (short) i                | int i        | (int) (short) i       ",
            // some primitive casts are not redundant, because of boxing
            " (Integer) (int) (char) c       | Character c  | (Integer) (int) c     ",
            " (Integer) (int) c              | Character c  | (Integer) (int) c     ",
            // one can not cast char -> Integer
            " (Integer) (int) (Character) c  | char c       | (Integer) (int) c     ",
        }
    )
    void testRemoveRedundantCasts(String expression, String arguments, String expected) {
        runExpressionTest(expression, arguments, expected, List.of(
            RemoveRedundantCasts.create()
        ));
    }
}
