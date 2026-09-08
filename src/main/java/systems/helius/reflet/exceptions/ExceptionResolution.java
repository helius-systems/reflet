package systems.helius.reflet.exceptions;

import systems.helius.reflet.accessors.Content;

import java.util.Collection;

/**
 * Describes the action selected by an {@link IntrospectionFailureHandler} for a given exception.
 */
public sealed interface ExceptionResolution permits ExceptionResolution.Skip,
        ExceptionResolution.Propagate,
        ExceptionResolution.Substitute {

    /**
     * Selects the outcome that drops the failed unit and keeps the search running.
     *
     * @return the shared skip resolution.
     */
    static Skip skip() {
        return Skip.INSTANCE;
    }

    /**
     * Selects the outcome that propagates the failure and aborts the search.
     *
     * @return the fatal resolution.
     */
    static Propagate propagate() {
        return Propagate.INSTANCE;
    }

    /**
     * Selects the outcome that feeds a replacement content into the search.
     *
     * @param content the replacement content.
     * @return a substitute resolution carrying {@code content}.
     */
    static Substitute substitute(Collection<Content> content) {
        return new Substitute(content);
    }


    /**
     * Resolution that drops the failed unit and continues the search.
     */
    final class Skip implements ExceptionResolution {
        private static final Skip INSTANCE = new Skip();

        private Skip() {
        }
    }

    /**
     * Resolution that propagates the failure and aborts the search.
     */
    final class Propagate implements ExceptionResolution {
        private static final Propagate INSTANCE = new Propagate();

        private Propagate() {
        }
    }

    /**
     * Resolution that supplies a replacement value for the failed unit.
     *
     * @param value the replacement value, or an empty collection to indicate no replacement (equivalent to skipping).
     */
    record Substitute(Collection<Content> value) implements ExceptionResolution {
    }
}
