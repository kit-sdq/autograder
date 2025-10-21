package de.firemage.autograder.extra.pmd;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.core.check.Check;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.reporting.RuleViolation;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;

import java.util.ArrayList;
import java.util.List;

public abstract class PMDCheck implements Check {
    private static final Language JAVA_LANGUAGE = LanguageRegistry.PMD.getLanguageById("java");
    private final List<Rule> rules;
    private final CustomExplanation explanation;
    private final ProblemType problemType;

    protected PMDCheck(Translatable explanation, Rule rule, ProblemType problemType) {
        this((ProblemRenderer renderer, RuleViolation violation) -> explanation, List.of(rule), problemType);
    }

    protected PMDCheck(CustomExplanation explanation, Rule rule, ProblemType problemType) {
        this(explanation, List.of(rule), problemType);
    }

    protected PMDCheck(CustomExplanation explanation, List<Rule> rules, ProblemType problemType) {
        this.explanation = explanation;
        this.rules = new ArrayList<>(rules);
        this.problemType = problemType;

        for (Rule rule : rules) {
            if (rule.getLanguage() == null) {
                rule.setLanguage(JAVA_LANGUAGE);
            }

            if (rule.getMessage() == null) {
                rule.setMessage("");
            }
        }
    }

    @Override
    public LocalizedMessage getLinter() {
        return new LocalizedMessage("linter-pmd");
    }

    public List<Rule> getRules() {
        return new ArrayList<>(rules);
    }

    public CustomExplanation getExplanation() {
        return this.explanation;
    }

    public ProblemType getProblemType() {
        return problemType;
    }
}
