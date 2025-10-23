package de.firemage.autograder.extra.pmd;

import de.firemage.autograder.api.Translatable;
import net.sourceforge.pmd.reporting.RuleViolation;

@FunctionalInterface
public interface CustomExplanation {
    /**
     * Builds the explanation for a given rule violation using the provided renderer.
     *
     * @param renderer the problem renderer for additional context like code snippets
     * @param violation the PMD rule violation to explain
     * @return a translatable explanation for the violation
     */
    Translatable apply(ProblemRenderer renderer, RuleViolation violation);
}
