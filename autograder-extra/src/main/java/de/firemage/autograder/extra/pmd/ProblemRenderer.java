package de.firemage.autograder.extra.pmd;

import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.file.SourceInfo;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.lang.document.TextFileContent;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import org.apache.commons.io.output.NullWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProblemRenderer extends AbstractIncrementingRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(ProblemRenderer.class);

    private final SourceInfo sourceInfo;
    private final Map<String, ? extends PMDCheck> checks;
    private final List<Problem> problems = new ArrayList<>();
    private Collection<TextFile> sourceFiles = new ArrayList<>();

    ProblemRenderer(Map<String, ? extends PMDCheck> checks, SourceInfo sourceInfo) {
        super("Custom renderer", "Creates InCodeProblems");
        this.checks = new HashMap<>(checks);
        this.sourceInfo = sourceInfo;
        super.setWriter(NullWriter.INSTANCE);
    }

    void setSourceFiles(List<? extends TextFile> sourceFiles) {
        this.sourceFiles = new ArrayList<>(sourceFiles);
    }

    /**
     * Returns the content of the file with the given file id.
     *
     * @param fileId the file id
     * @return an optional containing the file content, or an empty optional if the file was not found
     */
    public Optional<TextFileContent> getFileContent(FileId fileId) {
        return this.sourceFiles.stream()
            .filter(file -> file.getFileId().equals(fileId))
            .map(file -> {
                try {
                    return file.readContents();
                } catch (IOException exception) {
                    throw new IllegalStateException("failed to read content of file %s".formatted(fileId), exception);
                }
            })
            .findFirst();
    }

    @Override
    public void renderFileViolations(Iterator<RuleViolation> violations) {
        violations.forEachRemaining(violation -> {
            // TODO: Instead of catch-all + system.exit, use a field in this class to store exceptions and handle them in end?

            // NOTE: the caller of this method catches all exceptions, so if something crashes, it will not be
            //       visible without that printStackTrace
            try {
                this.problems.add(new PMDInCodeProblem(this.checks.get(violation.getRule().getName()), violation, this.sourceInfo, this));
            } catch (Exception exception) {
                exception.printStackTrace();
                // make sure the program stops running
                System.exit(-1);
            }
        });
    }

    @Override
    public String defaultFileExtension() {
        return null;
    }

    @Override
    public void end() {
        //TODO Don't ignore processing errors (via Report.ProcessingError)

        for (Report.ConfigurationError error : configErrors) {
            LOG.error("PMD config error: " + error.issue());
        }
    }

    @Override
    public void start() {
        // Do nothing for this renderer
    }

    @Override
    public void flush() {
        // Do nothing for this renderer
    }

    public List<Problem> getProblems() {
        return Collections.unmodifiableList(this.problems);
    }
}
