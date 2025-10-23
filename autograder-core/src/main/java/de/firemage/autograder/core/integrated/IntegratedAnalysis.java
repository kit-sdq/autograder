package de.firemage.autograder.core.integrated;

import de.firemage.autograder.api.FailureInformation;
import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.core.CodeLinter;
import de.firemage.autograder.core.LinterStatus;
import de.firemage.autograder.api.AbstractTempLocation;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IntegratedAnalysis implements CodeLinter<IntegratedCheck> {
    private static final boolean IS_IN_DEBUG_MODE = CoreUtil.isInDebugMode();
    private static final String INITIAL_INTEGRITY_CHECK_NAME = "StaticAnalysis-Constructor";
    private static final boolean ENSURE_NO_ORPHANS = false;
    private static final boolean ENSURE_NO_MODEL_CHANGES = false;
    private static final Logger logger = LoggerFactory.getLogger(IntegratedAnalysis.class);

    private UploadedFile file;
    private CtModel originalModel;
    private StaticAnalysis staticAnalysis;

    private void init(UploadedFile file) {
        this.file = file;

        // create a copy of the model to later check if a check changed the model
        if (IS_IN_DEBUG_MODE || ENSURE_NO_MODEL_CHANGES || ENSURE_NO_ORPHANS) {
            this.originalModel = file.copy().getModel().getModel();
        } else {
            this.originalModel = null;
        }

        this.staticAnalysis = new StaticAnalysis(file.getModel(), file.getCompilationResult());
        if (IS_IN_DEBUG_MODE && this.originalModel == this.staticAnalysis.getModel()) {
            throw new IllegalStateException("The model was not cloned");
        }

        this.assertModelIntegrity(INITIAL_INTEGRITY_CHECK_NAME);
    }

    @Override
    public Class<IntegratedCheck> supportedCheckType() {
        return IntegratedCheck.class;
    }

    @Override
    public List<Problem> lint(
        UploadedFile submission,
        AbstractTempLocation tempLocation,
        ClassLoader classLoader,
        List<? extends IntegratedCheck> checks,
        Consumer<? super Translatable> statusConsumer,
        Consumer<? super FailureInformation> failureConsumer
    ) {
        this.init(submission);

        statusConsumer.accept(LinterStatus.BUILDING_CODE_MODEL.getMessage());
        this.staticAnalysis.getCodeModel().ensureModelBuild();

        statusConsumer.accept(LinterStatus.RUNNING_INTEGRATED_CHECKS.getMessage());

        List<Problem> result = new ArrayList<>();
        for (IntegratedCheck check : checks) {
            long beforeTime = System.nanoTime();
            try {
                result.addAll(check.run(
                    this.staticAnalysis,
                    this.file.getSource()
                ));
            } catch (Exception exception) {
                failureConsumer.accept(new FailureInformation(
                    check.getClass().getSimpleName(),
                    exception
                ));
            }
            long afterTime = System.nanoTime();
            logger.info("Completed check " + check.getClass().getSimpleName() + " in " + ((afterTime - beforeTime) / 1_000_000 + "ms"));
            this.assertModelIntegrity(check.getClass().getSimpleName());
        }

        return result;
    }

    // sometimes spoon creates invalid elements, which are not the fault of this project or any check
    private static final Set<CtElement> alreadyInvalidElements = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * This method checks that a check did not change the model in a way that would influence other checks.
     *
     * @param checkName the check that was just executed
     */
    private void assertModelIntegrity(String checkName) {
        CtModel linterModel = this.staticAnalysis.getModel();

        if (ENSURE_NO_MODEL_CHANGES || IS_IN_DEBUG_MODE) {
            Collection<ParentChecker.InvalidElement> invalidElements = ParentChecker.checkConsistency(linterModel.getUnnamedModule());

            if (checkName.equals(INITIAL_INTEGRITY_CHECK_NAME)) {
                invalidElements.stream().map(ParentChecker.InvalidElement::element).forEach(alreadyInvalidElements::add);
            }

            invalidElements.removeIf(elem -> alreadyInvalidElements.contains(elem.element()));
            if (!invalidElements.isEmpty()) {
                throw new IllegalStateException("The model was modified by %s, %d elements have invalid parents:%n%s".formatted(
                    checkName,
                    invalidElements.size(),
                    invalidElements.stream().map(ParentChecker.InvalidElement::toString).limit(5).collect(Collectors.joining(System.lineSeparator()))
                ));
            }
        }

        if ((ENSURE_NO_MODEL_CHANGES || IS_IN_DEBUG_MODE) && !this.isModelEqualTo(this.originalModel, linterModel)) {
            throw new IllegalStateException("The model was changed by the check: %s".formatted(checkName));
        }

        if (ENSURE_NO_ORPHANS || IS_IN_DEBUG_MODE) {
            List<CtElement> orphans = findOrphans(linterModel);
            if (!orphans.isEmpty()) {
                throw new IllegalStateException(
                    "The check %s introduced new elements into the model without parents (did you forget to clone before passing the element to a setter?): %s".formatted(
                        checkName,
                        orphans.stream().map(element -> "%s(\"%s\")".formatted(element.getClass().getSimpleName(), element)).toList()
                    ));
            }
        }
    }

    private static final class ParentChecker extends CtScanner {
        private final List<InvalidElement> invalidElements;
        private final Deque<CtElement> stack;

        private ParentChecker() {
            this.invalidElements = new ArrayList<>();
            this.stack = new ArrayDeque<>();
        }

        public static List<InvalidElement> checkConsistency(CtElement ctElement) {
            ParentChecker parentChecker = new ParentChecker();
            parentChecker.scan(ctElement);
            return parentChecker.invalidElements;
        }

        @Override
        public void enter(CtElement element) {
            if (!this.stack.isEmpty() && (!element.isParentInitialized() || element.getParent() != this.stack.peek())) {
                this.invalidElements.add(new InvalidElement(element, this.stack));
            }


            this.stack.push(element);
        }

        @Override
        protected void exit(CtElement e) {
            this.stack.pop();
        }

        public record InvalidElement(CtElement element, Deque<CtElement> stack) {
            public InvalidElement {
                stack = new ArrayDeque<>(stack);
            }

            public String reason() {
                String name = this.element instanceof CtNamedElement ctNamedElement ? "-" + ctNamedElement.getSimpleName() : "";
                return (this.element.isParentInitialized() ? "inconsistent" : "null")
                    + " parent for " + this.element.getClass() + name
                    + " - " + this.element.getPosition()
                    + " - " + this.stack.peek();
            }

            public String dumpStack() {
                List<String> output = new ArrayList<>();

                for (CtElement ctElement : this.stack) {
                    output.add("    " + ctElement.getClass().getSimpleName()
                        + " " + (ctElement.getPosition().isValidPosition() ? String.valueOf(ctElement.getPosition()) : "(?)")
                    );
                }

                return String.join(System.lineSeparator(), output);
            }

            @Override
            public String toString() {
                return "%s%n%s".formatted(this.reason(), this.dumpStack());
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) {
                    return true;
                }
                if (!(object instanceof InvalidElement that)) {
                    return false;
                }

                return this.element == that.element();
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(this.element);
            }
        }
    }

    private boolean isModelEqualTo(CtModel original, CtModel toCheck) {
        return original.getUnnamedModule().equals(toCheck.getUnnamedModule());
    }

    private static boolean isOrphan(CtElement ctElement) {
        // I think those are created by the shadow model? => ignored
        if (ctElement instanceof CtPackageReference ctPackage && ctPackage.getQualifiedName().startsWith("java.")) {
            return false;
        }

        // TODO: these should not be ignored
        if (ctElement instanceof CtTypeReference<?> || ctElement instanceof CtLiteral<?> || ctElement instanceof CtPackageReference) {
            return false;
        }

        CtElement root = ctElement.getFactory().getModel().getUnnamedModule();
        if (root == ctElement) {
            return false;
        }

        CtElement parent = ctElement;
        Iterator<CtElement> iterator = ElementUtil.parents(ctElement).iterator();
        for (; iterator.hasNext(); parent = iterator.next()) ;

        return parent != root;
    }

    private static List<CtElement> findOrphans(CtModel ctModel) {
        return ctModel.getElements(IntegratedAnalysis::isOrphan);
    }

    public StaticAnalysis getStaticAnalysis() {
        return staticAnalysis;
    }
}
