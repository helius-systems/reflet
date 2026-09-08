package systems.helius.reflet.exceptions;

import java.util.function.Predicate;

/**
 * Policy invoked whenever an exception is thrown during content extraction or descent.
 */
@FunctionalInterface
public interface IntrospectionFailureHandler {

    /**
     * Resolves a thrown introspection exception.
     *
     * @param context the exception context.
     * @return the selected resolution.
     * @throws NullPointerException if {@code context} is {@code null}.
     */
    ExceptionResolution handle(ExceptionContext context);

    /**
     * Creates the default policy that propagates every exception.
     *
     * @return a handler that always returns {@link ExceptionResolution#propagate()}.
     */
    static IntrospectionFailureHandler propagateAll() {
        return context -> ExceptionResolution.propagate();
    }

    /**
     * Creates a policy that skips exceptions matched by {@code predicate} and propagates the rest.
     *
     * @param predicate the predicate used to decide which exceptions are skipped.
     * @return a handler that skips matching exceptions.
     */
    static IntrospectionFailureHandler skipIf(Predicate<Throwable> predicate) {
        return context -> predicate.test(context.thrown())
                ? ExceptionResolution.skip()
                : ExceptionResolution.propagate();
    }
}
