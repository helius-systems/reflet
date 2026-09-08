package systems.helius.reflet.exceptions;

/**
 * An exception thrown when an introspection has a fatal failure.
 */
public class IntrospectionException extends Exception {
    public IntrospectionException(TracedAccessException cause) {
        super(cause.buildPath());
        setStackTrace(cause.getStackTrace());
    }
}
