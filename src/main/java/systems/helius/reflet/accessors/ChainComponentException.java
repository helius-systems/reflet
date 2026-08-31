package systems.helius.reflet.accessors;

/**
 * Exception thrown by components of a responsibility chain.
 * It indicates that an exception occurred within this component,
 * and whether the process should fallback to the next component in the chain.
 */
public class ChainComponentException extends Exception {
    /**
     * Indicates whether another chain member should be allowed to attempt to continue the process.
     */
    protected final boolean allowFallback;

    public ChainComponentException(String message) {
        super(message);
        this.allowFallback = true; // Default to allowing delegation
    }

    public ChainComponentException(String message, Throwable cause) {
        super(message, cause);
        this.allowFallback = true; // Default to allowing delegation
    }

    public ChainComponentException(Throwable cause) {
        super(cause);
        this.allowFallback = true; // Default to allowing delegation
    }

    public ChainComponentException(String message, boolean allowFallback) {
        super(message);
        this.allowFallback = allowFallback;
    }

    public ChainComponentException(String message, Throwable cause, boolean allowFallback) {
        super(message, cause);
        this.allowFallback = allowFallback;
    }

    public ChainComponentException(Throwable cause, boolean allowFallback) {
        super(cause);
        this.allowFallback = allowFallback;
    }

    public boolean isAllowFallback() {
        return allowFallback;
    }
}
