package de.firemage.autograder.core.check.utils;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public sealed interface Option<T> extends Iterable<T> permits Option.Some, Option.None {
    static <T> Option<T> ofNullable(T value) {
        return value == null ? new None<>() : new Some<>(value);
    }

    static <T> Option<T> some(T value) {
        Objects.requireNonNull(value, "Value must not be null.");
        return new Some<>(value);
    }

    static <T> Option<T> none() {
        return new None<>();
    }

    default T orElseThrow() {
        if (this instanceof Some<T> someValue) {
            return someValue.value;
        } else if (this instanceof None<T>) {
            throw new IllegalStateException("Expected Some value, but got None.");
        }
        throw new IllegalArgumentException();
    }

    default boolean isSome() {
        return this instanceof Some;
    }

    default <U> Option<U> map(Function<T, U> function) {
        if (this instanceof Some<T>(T value)) {
            return new Some<>(function.apply(value));
        }

        if (this instanceof None<T>) {
            return new None<>();
        }

        throw new IllegalStateException();
    }

    /**
     * Returns the value if it is present or null if it is not.
     *
     * @return the value or null
     */
    default T nullable() {
        if (this instanceof Some<T>(T value)) {
            return value;
        }

        if (this instanceof None<T>) {
            return null;
        }
        throw new IllegalStateException();
    }

    default Stream<T> stream() {
        if (this instanceof Some<T> someValue) {
            return Stream.of(someValue.value);
        } else if (this instanceof None<T>) {
            return Stream.empty();
        }
        throw new IllegalStateException();
    }

    @Override
    default Iterator<T> iterator() {
        return stream().iterator();
    }

    record None<T>() implements Option<T> {
    }

    record Some<T>(T value) implements Option<T> {
    }
}
