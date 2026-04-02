package de.firemage.autograder.core.integrated;

import de.firemage.autograder.core.integrated.structure.StructuralElement;
import de.firemage.autograder.core.integrated.structure.StructuralEqualsVisitor;
import spoon.processing.AbstractProcessor;
import spoon.processing.FactoryAccessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.path.CtRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.StreamSupport;

public final class DuplicateCodeFinder {
    private static final String METADATA_KEY = "autograder_duplicate_code_uses";
    public static final BiPredicate<? super CtRole, Object> ALLOWED_DIFFERENCE = StructuralEqualsVisitor.ALLOW_NAME_DIFFERENCE
        .or(StructuralEqualsVisitor.ALLOW_REFACTORABLE_DIFFERENCE);

    private final Map<StructuralElement<CtStatement>, List<CtStatement>> occurrences;

    private DuplicateCodeFinder(CtModel model) {
        this.occurrences = new HashMap<>();
        model.processWith(new AbstractProcessor<CtStatement>() {
            @Override
            public void process(CtStatement ctStatement) {
                if (ctStatement.isImplicit() || !ctStatement.getPosition().isValidPosition()) {
                    return;
                }

                DuplicateCodeFinder.this.occurrences.computeIfAbsent(
                    new StructuralElement<>(ctStatement, ALLOWED_DIFFERENCE),
                    key -> new ArrayList<>()
                ).add(ctStatement);
            }
        });

        /*
        if (CoreUtil.isInDebugMode()) {
            Map<Integer, List<Object>> collisions = new HashMap<>();
            for (var key : this.occurrences.keySet()) {
                collisions.computeIfAbsent(key.hashCode(), k -> new ArrayList<>()).add(key);
            }

            var mostCommonCollisions = collisions.values()
                .stream()
                .filter(list -> list.size() > 1)
                .sorted((a, b) -> Integer.compare(b.size(), a.size()))
                .limit(10)
                .toList();

            int numberOfDuplicateHashCodes = this.occurrences.size() - collisions.size();

            if (numberOfDuplicateHashCodes > 20) {
                throw new IllegalStateException("Too many hash collisions %d of %d elements.".formatted(numberOfDuplicateHashCodes, this.occurrences.size()));
            }
        }*/
    }

    public static void buildFor(CtModel model) {
        DuplicateCodeFinder uses = new DuplicateCodeFinder(model);
        model.getRootPackage().putMetadata(METADATA_KEY, uses);
    }

    private static DuplicateCodeFinder getFor(FactoryAccessor factoryAccessor) {
        var uses = (DuplicateCodeFinder) ElementUtil.getRootPackage(factoryAccessor).getMetadata(METADATA_KEY);
        if (uses == null) {
            throw new IllegalArgumentException("No duplicate code uses information available for this model");
        }
        return uses;
    }

    private List<CtStatement> findDuplicateStatements(CtStatement statement) {
        return Collections.unmodifiableList(this.occurrences.get(new StructuralElement<>(statement, ALLOWED_DIFFERENCE)));
    }

    private static <K, V> Iterable<Map.Entry<K, V>> zip(Iterable<K> keys, Iterable<V> values) {
        return () -> new Iterator<>() {
            private final Iterator<K> keyIterator = keys.iterator();
            private final Iterator<V> valueIterator = values.iterator();

            @Override
            public boolean hasNext() {
                return this.keyIterator.hasNext() && this.valueIterator.hasNext();
            }

            @Override
            public Map.Entry<K, V> next() {
                return Map.entry(this.keyIterator.next(), this.valueIterator.next());
            }
        };
    }

    public record DuplicateCode(List<CtStatement> left, List<CtStatement> right) {
        public int size() {
            return this.left.stream().map(DuplicateCode::countStatements).mapToInt(Integer::intValue).sum();
        }

        private static int countStatements(CtStatement ctStatement) {
            int count = ctStatement.getElements(ctElement -> ctElement instanceof CtStatement
                && !(ctElement instanceof CtComment)
                && !(ctElement instanceof CtStatementList)
                && ctElement.getPosition().isValidPosition()
                && !ctElement.isImplicit()
            ).size();

            return Math.max(count, 1);
        }

        public boolean isMoreThanOrEqualTo(int threshold) {
            int size = 0;
            // TODO: this could be optimized in the future by stopping getElements when the threshold is reached
            for (var statement : this.left) {
                size += countStatements(statement);
                if (size >= threshold) {
                    return true;
                }
            }

            return Math.max(size, 1) >= threshold;
        }

        public List<StructuralEqualsVisitor.Difference> differences() {
            return StreamSupport.stream(zip(this.left, this.right).spliterator(), false)
                .flatMap(entry -> StructuralEqualsVisitor.findDifferences(entry.getKey(), entry.getValue(), ALLOWED_DIFFERENCE).stream())
                .toList();
        }
    }

    /**
     * Finds all duplicate code blocks with the given statement.
     * @param start the first statement of the code block
     * @return a list of all duplicate code blocks, the left will always contain the start statement and the right will be the duplicate
     */
    public static List<DuplicateCode> findDuplicates(CtStatement start) {
        DuplicateCodeFinder finder = DuplicateCodeFinder.getFor(start);

        List<DuplicateCode> result = new ArrayList<>();

        // we start searching for duplicates from the given statement
        for (CtStatement duplicate : finder.findDuplicateStatements(start)) {
            if (duplicate == start) {
                continue;
            }

            List<CtStatement> leftCode = new ArrayList<>(List.of(start));
            List<CtStatement> rightCode = new ArrayList<>(List.of(duplicate));

            for (var entry : zip(StatementUtil.getNextStatements(start), StatementUtil.getNextStatements(duplicate))) {
                if (!StructuralEqualsVisitor.equals(entry.getKey(), entry.getValue(), ALLOWED_DIFFERENCE)) {
                    break;
                }

                leftCode.add(entry.getKey());
                rightCode.add(entry.getValue());
            }

            result.add(new DuplicateCode(leftCode, rightCode));
        }

        return result;
    }
}
