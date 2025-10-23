package de.firemage.autograder.api;

import java.util.function.Consumer;

/**
 * Contains information about a failure that occurred during linting.
 *
 * @param name the name of the check that failed
 * @param exception the exception that was thrown
 */
public record FailureInformation(String name, Exception exception) {
    public static Consumer<FailureInformation> failFastConsumer() {
        return failure -> {
            throw new IllegalStateException("The check %s failed to execute".formatted(failure.name()), failure.exception());
        };
    }
}
