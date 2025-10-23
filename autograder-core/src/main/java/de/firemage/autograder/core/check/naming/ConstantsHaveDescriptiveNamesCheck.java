package de.firemage.autograder.core.check.naming;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.integrated.FactoryUtil;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import de.firemage.autograder.core.integrated.MethodUtil;
import de.firemage.autograder.core.integrated.TypeUtil;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ExecutableCheck(reportedProblems = {ProblemType.MEANINGLESS_CONSTANT_NAME, ProblemType.CONSTANT_NAME_CONTAINS_VALUE})
public class ConstantsHaveDescriptiveNamesCheck extends IntegratedCheck {
    private static final List<String> NUMBER_PRE_SUFFIXES =
            List.of("index", "number", "value", "argument", "element", "param", "parameter", "arg", "group", "constant", "value_of");

    private static final List<String> NON_DESCRIPTIVE_NAMES = List.of("error", "pattern", "regex", "symbol", "constant", "const", "compare", "linebreak");
    private static final Map<String, List<String>> SPECIAL_VALUE_MAPPING = Map.ofEntries(
        Map.entry("->", List.of("arrow")),
        Map.entry("-->", List.of("arrow"))
    );
    private static final List<String> CONTEXTUAL_WORDS = List.of(
        "space", "whitespace",
        "white_space",
        "empty",
        "blank"
    );

    private static final double CONTEXTUAL_WORD_THRESHOLD = 0.5;
    private static final int MAXIMUM_NAME_DIFFERENCE = 2;

    private static boolean isNonDescriptiveIntegerName(String name, int value) {
        if (NON_DESCRIPTIVE_NAMES.contains(name.toLowerCase())) {
            return true;
        }

        List<String> valueNameOptions = switch (value) {
            case 0 -> List.of("zero", "null", "zeroth", "first");
            case -1 -> List.of("minusone", "minus_one", "negative_one", "negativone", "neg_one", "negone");
            case 1 -> List.of("one", "second");
            case 2 -> List.of("two", "third");
            case 3 -> List.of("three", "fourth");
            case 4 -> List.of("four", "fifth");
            case 5 -> List.of("five", "sixth");
            case 6 -> List.of("six", "seventh");
            case 7 -> List.of("seven", "eighth");
            case 8 -> List.of("eight", "ninth");
            case 9 -> List.of("nine", "tenth");
            default -> List.of();
        };
        return valueNameOptions.stream()
                .flatMap(o -> Stream.concat(Stream.of(o),
                        NUMBER_PRE_SUFFIXES.stream().flatMap(s -> Stream.of(s + "_" + o, o + "_" + s))))
                .anyMatch(o -> name.toLowerCase().equals(o));
    }

    private static boolean isNonDescriptiveStringName(String name, String value) {
        String cleanedName = name.toLowerCase().replace("_", "");
        if (NON_DESCRIPTIVE_NAMES.contains(cleanedName)) {
            return true;
        }

        List<String> options = new ArrayList<>();
        options.add(""); // Empty string is prefix of everything

        if (value.isEmpty()) {
            options = List.of("empty", "blank");
        } else {
            // ignore small values like PLAYER_SUFFIX = "P"
            if (value.length() < 2) {
                return false;
            }

            for (char c : value.toCharArray()) {
                var charOptions = listCharOptions(c);
                if (charOptions == null) {
                    return false;
                }
                options = options
                        .stream()
                        .flatMap(suffix -> charOptions.stream().map(o -> suffix + o))
                        .filter(cleanedName::startsWith)
                        .toList();

                if (options.isEmpty()) {
                    // Matching no longer possible, since we only ever add suffixes to the options
                    return false;
                }
            }
        }

        return options.contains(cleanedName);
    }

    private static List<String> listCharOptions(char c) {
        return switch (c) {
            case ' ' -> List.of("space", "whitespace", "white_space", "empty", "blank");
            case ',' -> List.of("comma", "punctuation");
            case '.' -> List.of("point", "dot", "fullstop", "full_stop", "punctuation");
            case '-' -> List.of("minus", "hyphen", "dash", "line");
            case ':' -> List.of("colon");
            case ';' -> List.of("semi_colon", "semicolon");
            case '_' -> List.of("underscore", "dash", "line");
            case '/', '\\' -> List.of("slash", "backslash");
            case '[', ']' -> List.of("bracket");
            case '*' -> List.of("star", "asterisk", "multiplication");
            case '+' -> List.of("plus");
            case '=' -> List.of("equals", "equal");
            default -> Character.isAlphabetic(c) ? List.of(String.valueOf(Character.toLowerCase(c))) : null;
        };
    }

    private static boolean containsValueInName(String name, CtLiteral<?> value) {
        String lowerCaseName = name.toLowerCase();

        // convert the value to a lowercase string (makes it easier to compare)
        String valueString = "null";
        if (value.getValue() != null) {
            valueString = value.getValue().toString().toLowerCase();
        }

        // ignore empty strings, which are always contained in every string
        if (valueString.isEmpty()) {
            return false;
        }

        if (valueString.length() == 1 && Character.isAlphabetic(valueString.charAt(0))) {
            String c = String.valueOf(valueString.charAt(0));
            return lowerCaseName.startsWith(c + "_") || lowerCaseName.endsWith("_" + c) || lowerCaseName.contains("_" + c + "_");
        }

        // convert special character values like ":" to their names (colon)
        if (valueString.length() == 1 && !Character.isAlphabetic(valueString.charAt(0))) {
            List<String> charOptions = listCharOptions(valueString.charAt(0));
            if (charOptions != null) {
                for (var option : charOptions) {
                    for (String word : name.split("_")) {
                        word = word.toLowerCase();

                        // some words should be allowed in a context, like space between should be fine when the value is a space.
                        if (CONTEXTUAL_WORDS.contains(word) && (word.length() * 1.0) / (lowerCaseName.length() * 1.0) < CONTEXTUAL_WORD_THRESHOLD) {
                            return false;
                        }

                        if (word.equals(option)) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        for (var entry : SPECIAL_VALUE_MAPPING.entrySet()) {
            if (valueString.contains(entry.getKey())) {
                return entry.getValue().stream().anyMatch(lowerCaseName::contains);
            }
        }

        // to detect private static String MY_CONSTANT = "my-constant"
        valueString = valueString.replace('-', '_');

        if (lowerCaseName.contains(valueString)) {
            String newName = lowerCaseName.replace(valueString, "");

            // we do not want to detect constants like `QUIT_COMMAND_NAME = "quit"`, so we only complain
            // if the name is almost the same as the value
            return newName.length() <= MAXIMUM_NAME_DIFFERENCE;
        }

        return false;
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtField<?>>() {
            @Override
            public void process(CtField<?> field) {
                if (field.isImplicit() || !field.getPosition().isValidPosition()) {
                    return;
                }

                if (!field.isFinal() || field.getDefaultExpression() == null) {
                    return;
                }

                CtLiteral<?> literal;
                if (field.getDefaultExpression() instanceof CtLiteral<?> ctLiteral) {
                    literal = ctLiteral;
                } else if (field.getDefaultExpression() instanceof CtInvocation<?> ctInvocation
                    // check if the value is System.lineSeparator()
                    && ctInvocation.getTarget() instanceof CtTypeAccess<?> ctTypeAccess
                    && TypeUtil.isTypeEqualTo(ctTypeAccess.getAccessedType(), java.lang.System.class)
                    && MethodUtil.isSignatureEqualTo(ctInvocation.getExecutable(), String.class, "lineSeparator")) {
                    literal = FactoryUtil.makeLiteral(field.getFactory().Type().stringType(), "\n");
                } else {
                    return;
                }

                String fieldName = field.getSimpleName();

                if (literal.getValue() instanceof Integer v1 && isNonDescriptiveIntegerName(fieldName, v1)
                    || literal.getValue() instanceof String v2 && isNonDescriptiveStringName(fieldName, v2)) {
                    addLocalProblem(
                        field,
                        new LocalizedMessage(
                            "constants-name-exp",
                            Map.of(
                                "name", field.getSimpleName(),
                                "value", field.getDefaultExpression()
                            )
                        ),
                        ProblemType.MEANINGLESS_CONSTANT_NAME
                    );
                } else if (containsValueInName(fieldName, literal)) {
                    addLocalProblem(
                        field,
                        new LocalizedMessage(
                            "constants-name-exp-value",
                            Map.of(
                                "name", field.getSimpleName(),
                                "value", field.getDefaultExpression()
                            )
                        ),
                        ProblemType.CONSTANT_NAME_CONTAINS_VALUE
                    );
                }
            }
        });
    }

    @Override
    public Optional<Integer> maximumProblems() {
        return Optional.of(6);
    }
}
