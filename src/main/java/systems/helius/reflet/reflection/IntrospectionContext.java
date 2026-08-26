package systems.helius.reflet.reflection;

import systems.helius.reflet.reflection.accessors.ContentAccessor;

import java.lang.invoke.MethodHandles;
import java.util.Set;

/**
 * Describes the constant elements that are referred to regularly during an introspection.
 * @param targetType the class of the type of values we seek
 * @param rootLookup the lookup that was provided at the start of the search
 * @param found all values that have been found thus far that are of the right type
 * @param visited all visited objects
 * @param <T> the target type
 */
public record IntrospectionContext<T>(Class<T> targetType,
                                      MethodHandles.Lookup rootLookup,
                                      Set<T> found,
                                      Set<Object> visited,
                                      ContentAccessor contentAccessor) {
}
