package de.firemage.autograder.core;

import de.firemage.autograder.api.AbstractLinter;
import de.firemage.autograder.api.AbstractProblemType;
import de.firemage.autograder.api.CheckConfiguration;
import de.firemage.autograder.api.FailureInformation;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.core.check.Check;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.file.SourcePath;
import de.firemage.autograder.core.file.TempLocation;
import de.firemage.autograder.core.file.UploadedFile;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Linter implements AbstractLinter {
    private final int threads;
    private final TempLocation tempLocation;
    private final ClassLoader classLoader;
    private final int maxProblemsPerCheck;
    private final Translations translations;

    public static Linter defaultLinter(Locale locale) {
        return new Linter(AbstractLinter.builder(locale));
    }

    public Linter(
        AbstractLinter.Builder builder
    ) {
        this.translations = new Translations(builder.getLocale(), builder.getMessageOverrides(), builder.getConditionalOverrides());
        this.tempLocation = builder.getTempLocation() != null ? (TempLocation) builder.getTempLocation() : TempLocation.random();
        this.threads = builder.getThreads();
        this.classLoader = builder.getClassLoader();
        this.maxProblemsPerCheck = builder.getMaxProblemsPerCheck();
    }

    public Translations getTranslations() {
        return translations;
    }

    @Override
    public List<Problem> checkFileFallible(Path file, JavaVersion version, CheckConfiguration checkConfiguration,
                                                             Consumer<Translatable> statusConsumer,
                                                             Consumer<FailureInformation> failureConsumer)
        throws LinterException, IOException {
        try (var uploadedFile = UploadedFile.build(file, version, this.tempLocation, statusConsumer, this.classLoader)) {
            return this.checkFileFallible(uploadedFile, checkConfiguration, statusConsumer, failureConsumer);
        }
    }

    public List<Problem> checkFileFallible(
        UploadedFile file,
        CheckConfiguration checkConfiguration,
        Consumer<Translatable> statusConsumer,
        Consumer<FailureInformation> failureConsumer
    ) throws LinterException, IOException {
        var checks = this.findChecksForProblemTypes(checkConfiguration.problemsToReport());
        return this.checkFileFallible(file, checkConfiguration, checks, statusConsumer, failureConsumer);
    }

    private static List<Problem> filterProblematicAnnotations(Iterable<? extends Problem> problems) {
        // HACK: for the following issue:
        //       https://github.com/Feuermagier/autograder/issues/672

        List<Problem> problematicAnnotations = new ArrayList<>();
        List<Problem> result = new ArrayList<>();
        for (Problem problem : problems) {
            // skip unrelated problems:
            if (!Set.of(ProblemType.UNUSED_DIAMOND_OPERATOR, ProblemType.UNCHECKED_TYPE_CAST, ProblemType.DO_NOT_USE_RAW_TYPES).contains(problem.getProblemType())) {
                result.add(problem);
                continue;
            }

            problematicAnnotations.add(problem);
        }

        record CodeByLineKey(SourcePath file, int startLine) {
            CodeByLineKey(CodePosition position) {
                this(position.file(), position.startLine());
            }
        }

        Map<CodeByLineKey, List<Problem>> problemsByLine = new HashMap<>();
        for (Problem problem : problematicAnnotations) {
            CodeByLineKey position = new CodeByLineKey(problem.getPosition());
            problemsByLine.computeIfAbsent(position, key -> new ArrayList<>()).add(problem);
        }

        for (var entry : problemsByLine.entrySet()) {
            if (entry.getValue().size() == 1) {
                result.add(entry.getValue().getFirst());
                continue;
            }

            // at least two problems on the same line
            // check if they are all of the three problems:
            Set<ProblemType> problemTypes = entry.getValue()
                .stream()
                .map(Problem::getProblemType)
                .collect(Collectors.toSet());

            if (problemTypes.equals(Set.of(ProblemType.UNUSED_DIAMOND_OPERATOR, ProblemType.UNCHECKED_TYPE_CAST, ProblemType.DO_NOT_USE_RAW_TYPES))) {
                // only keep the raw type annotations
                entry.getValue()
                    .stream()
                    .filter(problem -> problem.getProblemType() == ProblemType.DO_NOT_USE_RAW_TYPES)
                    .forEach(result::add);
            } else {
                // keep all problems
                result.addAll(entry.getValue());
            }
        }

        return result;
    }

    public List<Problem> checkFile(
        UploadedFile file,
        CheckConfiguration checkConfiguration,
        Iterable<? extends Check> checks,
        Consumer<Translatable> statusConsumer
    ) throws IOException {
        return this.checkFileFallible(file, checkConfiguration, checks, statusConsumer, FailureInformation.failFastConsumer());
    }

    private List<Problem> checkFileFallible(
        UploadedFile file,
        CheckConfiguration checkConfiguration,
        Iterable<? extends Check> checks,
        Consumer<Translatable> statusConsumer,
        Consumer<FailureInformation> failureConsumer
    ) throws IOException {
        // the file is null if the student did not upload source code
        if (file == null) {
            return new ArrayList<>();
        }

        Map<CodeLinter<?>, List<Check>> linterChecks = new IdentityHashMap<>();

        List<? extends CodeLinter<?>> codeLinters = this.findCodeLinter();
        for (Check check : checks) {
            for (CodeLinter<?> linter : codeLinters) {
                if (linter.supportedCheckType().isInstance(check)) {
                    linterChecks.computeIfAbsent(linter, key -> new ArrayList<>()).add(check);
                    // only add each check to one linter
                    break;
                }
            }
        }

        List<Problem> unreducedProblems = new ArrayList<>();
        try (TempLocation tempLinterLocation = this.tempLocation.createTempDirectory("linter")) {
            for (var entry : linterChecks.entrySet()) {
                CodeLinter linter = entry.getKey();
                var targetCheckType = linter.supportedCheckType();
                var associatedChecks = castUnsafe(entry.getValue(), targetCheckType);

                // skip linting if there are no checks for this linter
                // some linters take a long time to start, even if there are no checks to run
                if (associatedChecks.isEmpty()) {
                    continue;
                }

                unreducedProblems.addAll(linter.lint(
                    file,
                    tempLinterLocation,
                    this.classLoader,
                    associatedChecks,
                    statusConsumer,
                    failureConsumer
                ));
            }
        }

        unreducedProblems = filterProblematicAnnotations(unreducedProblems);

        if (!checkConfiguration.problemsToReport().isEmpty()) {
            unreducedProblems = unreducedProblems
                .stream()
                .filter(problem -> checkConfiguration.problemsToReport().contains(problem.getProblemType()))
                .toList();
        }

        // filter out problems in excluded classes
        var excludedClasses = checkConfiguration.excludedClasses();
        if (excludedClasses != null && !excludedClasses.isEmpty()) {
            unreducedProblems = unreducedProblems.stream()
                    .filter(problem -> !checkConfiguration.excludedClasses()
                            .contains(problem.getPosition().file().getName().replace(".java", "")))
                    .toList();
        }

        return this.mergeProblems(unreducedProblems);
    }

    private List<Problem> mergeProblems(Collection<? extends Problem> unreducedProblems) {
        // -1 means no limit (useful for unit tests, where one wants to see all problems)
        if (this.maxProblemsPerCheck == -1) {
            return new ArrayList<>(unreducedProblems);
        }

        // first group all problems by the check that created them
        Map<Check, List<Problem>> problems = unreducedProblems.stream()
            .collect(Collectors.groupingBy(Problem::getCheck, LinkedHashMap::new, Collectors.toList()));

        List<Problem> result = new ArrayList<>();
        for (Map.Entry<Check, List<Problem>> entry : problems.entrySet()) {
            Check check = entry.getKey();
            List<Problem> problemsForCheck = entry.getValue();

            int targetNumberOfProblems = Math.min(
                this.maxProblemsPerCheck,
                entry.getKey().maximumProblems().orElse(this.maxProblemsPerCheck)
            );

            // then go through each check and merge the problems if they exceed the maxProblemsPerCheck
            if (problemsForCheck.size() > targetNumberOfProblems) {
                // further partition the problems by their ProblemType
                // (one does not want to merge different types of problems):
                Map<ProblemType, List<Problem>> problemsByType = problemsForCheck.stream()
                    .collect(Collectors.groupingBy(Problem::getProblemType, LinkedHashMap::new, Collectors.toList()));

                problemsForCheck = problemsByType.values()
                    .stream()
                    .flatMap(list -> check.merge(list, targetNumberOfProblems).stream())
                    .collect(Collectors.toCollection(ArrayList::new));
            }

            result.addAll(problemsForCheck);
        }

        return result;
    }

    @Override
    public String translateMessage(Translatable message) {
        return message.format(this.translations);
    }

    private static final Collection<Class<?>> CHECKS = new LinkedHashSet<>(
            new Reflections(new ConfigurationBuilder()
                    .forPackage("de.firemage.autograder", Linter.class.getClassLoader())
                    .addClassLoaders(Linter.class.getClassLoader())
                    .setScanners(Scanners.TypesAnnotated)
            ).getTypesAnnotatedWith(ExecutableCheck.class)
    );

    public List<Check> findChecksForProblemTypes(Collection<? extends AbstractProblemType> problems) {
        return CHECKS
            .stream()
            .filter(check -> isRequiredCheck(check.getAnnotation(ExecutableCheck.class), problems))
            .map(check -> {
                try {
                    return (Check) check.getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to instantiate check " + check.getName(), e);
                } catch (ClassCastException e) {
                    throw new IllegalStateException(check.getName() + " does not inherit from Check");
                }
            })
            .toList();
    }

    private static final Collection<Class<?>> CODE_LINTER = new LinkedHashSet<>(
            new Reflections(new ConfigurationBuilder()
                    .forPackage("de.firemage.autograder", Linter.class.getClassLoader())
                    .addClassLoaders(Linter.class.getClassLoader())
                    .setScanners(Scanners.SubTypes)
            ).getSubTypesOf(CodeLinter.class)
    );

    public List<? extends CodeLinter<?>> findCodeLinter() {
        return CODE_LINTER
            .stream()
            .map(linter -> {
                try {
                    return (CodeLinter<?>) linter.getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to instantiate check " + linter.getName(), e);
                } catch (ClassCastException e) {
                    throw new IllegalStateException(linter.getName() + " does not inherit from Check");
                }
            })
            .toList();
    }


    private boolean isRequiredCheck(ExecutableCheck check, Collection<? extends AbstractProblemType> problems) {
        return check.enabled() && problems.stream().anyMatch(p -> List.of(check.reportedProblems()).contains(p));
    }

    private static <T> List<T> castUnsafe(Iterable<?> list, Class<? extends T> clazz) {
        List<T> result = new ArrayList<>();

        for (Object object : list) {
            result.add(clazz.cast(object));
        }

        return result;
    }
}
