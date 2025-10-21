package de.firemage.autograder.core.check.comment;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StatementUtil;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ExecutableCheck(reportedProblems = { ProblemType.UNNECESSARY_COMMENT })
public class UnnecessaryComment extends IntegratedCheck {
    private final Set<CtComment> visitedComments = Collections.newSetFromMap(new IdentityHashMap<>());

    private void checkComments(Collection<? extends CtComment> comments) {
        this.visitedComments.addAll(comments);

        if (comments.size() != 1) {
            return;
        }

        for (CtComment ctComment : comments) {
            if (ctComment.getContent().isBlank() && !(ctComment instanceof CtJavaDoc ctJavaDoc && ctJavaDoc.getJavadocElements().isEmpty())) {
                addLocalProblem(
                    ctComment,
                    new LocalizedMessage("unnecessary-comment-empty"),
                    ProblemType.UNNECESSARY_COMMENT
                );
                break;
            }
        }
    }

    private static boolean isStandaloneComment(CtComment ctComment) {
        return ctComment.getParent() instanceof CtElement ctElement && !ctElement.getComments().contains(ctComment);
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<>() {
            @Override
            public void process(CtElement element) {
                if (element.isImplicit() || !element.getPosition().isValidPosition()) {
                    return;
                }

                // The main problem is that one can use empty comments as spacers in larger comments.
                //
                // ^ like this.
                //
                // And to detect this correctly, we need to check the context of the comment as well
                // (if there are other comments around it).

                // Comments can appear either attached to a CtElement or as a standalone comment.
                //
                // Here the comments without an attached CtElement are processed:
                if (element instanceof CtComment ctComment && isStandaloneComment(ctComment) && !visitedComments.contains(ctComment)) {
                    List<CtComment> followingComments = StatementUtil.getNextStatements(ctComment)
                        .stream()
                        .takeWhile(CtComment.class::isInstance)
                        .map(CtComment.class::cast)
                        .collect(Collectors.toCollection(ArrayList::new));

                    followingComments.addFirst(ctComment);

                    checkComments(followingComments);
                    return;
                }

                if (!element.getComments().isEmpty()) {
                    checkComments(element.getComments());
                }
            }
        });
    }
}
