package systems.helius.reflet.exceptions;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Describes the context where an exception was raised during content extraction or descent.
 *
 * @param thrown       the exception being resolved.
 * @param owner        the object whose extraction or descent failed.
 * @param holdingField the field or relationship that failed, or {@code null} at the root.
 * @param targetType   the type being sought by the search.
 * @param origin       the boundary that caught the exception.
 */
public record ExceptionContext(Throwable thrown,
                               Object owner,
                               @Nullable Field holdingField,
                               Class<?> targetType,
                               Origin origin) {
    /**
     * Identifies the boundary that caught an introspection exception.
     */
    public enum Origin {
        /**
         * The failure happened while reading an individual field, collection element, or map entry.
         */
        ACCESSOR,

        /**
         * The failure reached the recursive descent backstop.
         */
        DESCENT
    }
}
