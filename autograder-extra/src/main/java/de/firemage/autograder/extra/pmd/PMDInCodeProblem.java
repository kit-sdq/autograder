package de.firemage.autograder.extra.pmd;

import de.firemage.autograder.core.CodePosition;
import de.firemage.autograder.core.LocalizedMessageForProblem;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.file.SourceInfo;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.nio.file.Path;

public class PMDInCodeProblem extends Problem {
    PMDInCodeProblem(PMDCheck check, RuleViolation violation, SourceInfo sourceInfo, ProblemRenderer renderer) {
        super(check,
                new CodePosition(
                        sourceInfo,
                        sourceInfo.getCompilationUnit(Path.of(violation.getFileId().getOriginalPath())).path(),
                        violation.getBeginLine(),
                        violation.getBeginLine(),
                        violation.getBeginColumn(),
                        violation.getBeginColumn()),
                new LocalizedMessageForProblem(
                        check.getExplanation() != null ? check.getExplanation().apply(renderer, violation) : new LocalizedMessage(violation.getDescription()),
                        check.getProblemType()),
                check.getProblemType());
    }

    @Override
    public String toString() {
        return "PMDInCodeProblem[check=%s, position=%s, explanation=%s, problemType=%s]".formatted(
                this.getCheck(),
                this.getPosition(),
                this.getExplanation(),
                this.getProblemType()
        );
    }
}
