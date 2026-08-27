package systems.helius.reflet.exceptions;

import java.io.Serial;

/**
 * Exception thrown when trying to lookup data, but some mechanism (such as module isolation)
 * prevents the lookup.
 */
public class LoookupAcquisitionException extends IllegalAccessException {
    @Serial
    private static final long serialVersionUID = 1L;

    public LoookupAcquisitionException(String message) {
        super(message);
    }

    /**
     * Create a LookupAcquisitionException with a specific cause.
     */
    public LoookupAcquisitionException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
