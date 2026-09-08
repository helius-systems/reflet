package systems.helius.reflet.exceptions;

import jakarta.annotation.Nullable;

import java.io.Serial;

public class AccessorException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Indicates that this exception is fatal and shall not be handled.
     */
    protected final boolean fatal;


    public AccessorException(String message) {
        super(message);
        this.fatal = false;
    }

    public AccessorException(String message, Throwable cause) {
        super(message, cause);
        this.fatal = false;
    }

    protected AccessorException(String message, Throwable cause, boolean fatal) {
        super(message, cause);
        this.fatal = fatal;
    }

    /**
     * Creates a new exception that is marked as fatal, and thus shall not be handled.
     *
     * @param message the message describing the failure.
     * @param cause   the failure cause that must be propagated.
     * @return a new exception marked as fatal.
     */
    public static AccessorException fatal(String message, @Nullable Throwable cause) {
        return new AccessorException(message, cause, true);
    }

    /**
     * Indicates whether this exception is fatal without another handler decision.
     *
     * @return {@code true} if the exception is fatal, {@code false} otherwise.
     */
    public final boolean isFatal() {
        return fatal;
    }
}
