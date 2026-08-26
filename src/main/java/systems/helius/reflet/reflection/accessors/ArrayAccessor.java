package systems.helius.reflet.reflection.accessors;

import jakarta.annotation.Nullable;
import systems.helius.reflet.reflection.IntrospectionContext;
import systems.helius.reflet.reflection.IntrospectionSettings;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Accessor specialized for raw arrays.
 */
public class ArrayAccessor implements ContentAccessor {
    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return current.isArray();
    }

    @Override
    public Collection<Content> extract(Object current, @Nullable Field holdingField, IntrospectionContext<?> context, IntrospectionSettings settings) throws ChainComponentException {
        Stream<?> source;
        if (current.getClass().getComponentType().isPrimitive()) {
            final int LENGTH = Array.getLength(current);
            ArrayList<Object> list = new ArrayList<>(LENGTH);
            for (int i = 0; i < LENGTH; i++)
                list.add(Array.get(current, i));
            source = list.stream();
            holdingField = SyntheticPrimitiveFields.getSyntheticPrimitiveField(current.getClass().getComponentType());
        } else {
            source = Arrays.stream((Object[]) current);
        }
        final Field resultingField = holdingField;
        return source.map(value -> new Content(value, resultingField))
                .toList();
    }
}
