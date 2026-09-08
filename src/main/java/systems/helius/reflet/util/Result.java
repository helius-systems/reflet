package systems.helius.reflet.util;

import jakarta.annotation.Nullable;


import java.util.Objects;
import java.util.Optional;

/**
 * A Result type that encapsulate either a value or an exception, depending upon the result of a function.
 *
 * @param <V> type of the value
 * @param <E> type of the exception
 */
public final class Result<V, E> {
    @Nullable
    private final V value;
    @Nullable
    private final E error;

    private Result(@Nullable V value, @Nullable E error) {
        if (value != null && error != null) {
            throw new IllegalArgumentException("Result cannot have both value and error");
        }
        if (value == null && error == null) {
            throw new IllegalArgumentException("Result must have either value or error");
        }
        this.value = value;
        this.error = error;
    }

    /**
     * Creates a success result.
     *
     * @param value the value to return
     * @param <V>   type of the result
     * @param <E>   type of the exception, if it had happened
     * @return a successful result
     */
    public static <V, E> Result<V, E> ok(V value) {
        return new Result<>(value, null);
    }

    /**
     * Creates a failure result.
     *
     * @param error the exception to return
     * @param <V>   type of the result, if it had succeeded
     * @param <E>   type of the exception
     * @return a failed result
     */
    public static <V, E> Result<V, E> err(E error) {
        return new Result<>(null, error);
    }

    public boolean isOk() {
        return !isErr(); // Because of the scenario where the result type is Void, we can use this to check if the result is successful.
    }

    public boolean isErr() {
        return error != null;
    }

    public Optional<V> value() {
        return Optional.ofNullable(value);
    }

    public Optional<E> error() {
        return Optional.ofNullable(error);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Result<?, ?> result = (Result<?, ?>) o;
        return Objects.equals(value, result.value) && Objects.equals(error, result.error);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(error);
        return result;
    }

    @Override
    public String toString() {
        return "Result{" +
                (value != null ? "value=" + value : "") +
                (error != null ? ", error=" + error : "") +
                '}';
    }
}