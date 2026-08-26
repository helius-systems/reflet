package systems.helius.reflet.reflection;

import jakarta.annotation.Nullable;
import systems.helius.reflet.exceptions.IntrospectionException;

import java.io.Serial;
import java.lang.reflect.Field;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Exception that collects the trace of fields traversed to get to the exception point.
 */
public class TracedAccessException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    @Nullable
    private Object root;
    private final Deque<Field> trace = new LinkedList<>();

    public TracedAccessException(String message) {
        super(message);
    }

    public TracedAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public TracedAccessException(Throwable cause) {
        super(cause);
    }

    /**
     * Add a field to the trace.
     * @param step the field that was traversed. If NULL, nothing is done.
     */
    public void addStep(@Nullable Field step) {
        if (step != null)
            trace.push(step);
    }

    /**
     * Builds a string representation of the path leading to the field that caused the access issue.
     * This includes the root object and all steps taken in the trace.
     * @return a string representation of the path.
     */
    public String buildPath() {
        StringBuilder sb = new StringBuilder(getMessage());
        sb.append("Path leading to issue: ");
        if (root != null) {
            sb.append("[root]: ");
            sb.append(root.getClass().getCanonicalName());
            sb.append(": ");
            sb.append(root);
        }
        for (Field step : trace) {
            sb.append(step.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * The object at the root of the search.
     * <p>
     *     Is null if the unwinding is not finished.
     * @return the root of the search that failed.
     */
    @Nullable
    public Object getRoot() {
        return root;
    }

    /**
     * The object at the root of the search.
     * @param root the object at the root of the search.
     */
    public void setRoot(@Nullable Object root) {
        this.root = root;
    }

    /**
     * Transform this traced exception back into a regular IllegalAccessException as if it happened at the location of this exception.
     * @return an IllegalAccessException with a message detailing the path to the illegal access.
     * @deprecated Use {@link IntrospectionException} instead, which is the official exception type for introspection errors in Helius reflet.
     */
    @Deprecated(since = "0.5.0") // Use IntrospectionException instead
    public IllegalAccessException toIllegalAccessException() {
        var exception = new IllegalAccessException(buildPath());
        exception.setStackTrace(getStackTrace());
        return exception;
    }
}
