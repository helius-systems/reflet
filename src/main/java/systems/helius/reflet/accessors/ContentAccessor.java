package systems.helius.reflet.accessors;

import jakarta.annotation.Nullable;
import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;
import systems.helius.reflet.exceptions.AccessorException;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Provides access to the content of an Object.
 * When a ContentAccessor accepts a type and field combo, no other ContentAccessors will be queried for it.
 */
public interface ContentAccessor {
    /**
     * Checks if this accessor accepts the current value.
     *
     * @param current      the class of the current value to access the innards of.
     * @param holdingField the field that contained the current value.
     *                     Null when current is the root of the search.
     * @return true if this accessor handles the current class for the given field, false otherwise.
     */
    boolean accepts(Class<?> current, @Nullable Field holdingField);

    /**
     * Extract the values present within the current object.
     * <p>
     * There may be more than one Content instance per field, and thus multiple values for a single field.
     * This is particularly relevant for collections and the likes.
     * <p>
     * Static fields should be ignored, as they are not part of the instance's content.
     *
     * @implSpec Implementations should not return null, but an empty collection if there are no values to extract.
     * @param current      the current value to access the innards of.
     * @param holdingField the field that contained the current value.
     *                     Null when current is the root of the search.
     * @param context      the current introspection context
     * @param settings     settings of the current search
     * @return a collection of the values within the current object.
     * This collection is not obligated to represent every single field within the object,
     * it contains what matters to look into.
     * @throws AccessorException in case of failure.
     */
    Collection<Content> extract(Object current,
                                @Nullable Field holdingField,
                                IntrospectionContext<?> context,
                                IntrospectionSettings settings) throws AccessorException;
}
