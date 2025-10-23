package de.firemage.autograder.extra.check.complexity;

import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.extra.pmd.PMDCheck;
import de.firemage.autograder.extra.pmd.ProblemRenderer;
import net.sourceforge.pmd.lang.document.TextFileContent;
import net.sourceforge.pmd.lang.java.rule.codestyle.UseDiamondOperatorRule;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.util.Map;
import java.util.Optional;

@ExecutableCheck(reportedProblems = {ProblemType.UNUSED_DIAMOND_OPERATOR})
public class DiamondOperatorCheck extends PMDCheck {
    public DiamondOperatorCheck() {
        super(
            DiamondOperatorCheck::parseRuleViolation,
            new UseDiamondOperatorRule(),
            ProblemType.UNUSED_DIAMOND_OPERATOR
        );
    }

    private static Translatable parseRuleViolation(ProblemRenderer renderer, RuleViolation ruleViolation) {
        TextFileContent source = renderer.getFileContent(ruleViolation.getFileId())
            .orElseThrow(() -> new IllegalStateException("Could not find source file for rule violation %s".formatted(ruleViolation)));

        var textRegion = ruleViolation.getLocation().getRegionInFile();
        if (textRegion == null) {
            throw new IllegalStateException("Could not find text region for rule violation %s".formatted(ruleViolation));
        }

        return new LocalizedMessage("use-diamond-operator", Map.of(
            "type", source.getNormalizedText().slice(textRegion)
        ));
    }

    @Override
    public Optional<Integer> maximumProblems() {
        return Optional.of(3);
    }
}
