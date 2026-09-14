package systems.helius.reflet.exceptions;

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
}
