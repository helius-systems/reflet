package systems.helius.reflet.reflection.accessors;

import jakarta.annotation.Nullable;
import systems.helius.reflet.reflection.IntrospectionContext;
import systems.helius.reflet.reflection.IntrospectionSettings;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterativeAccessor implements ContentAccessor {
    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return Iterable.class.isAssignableFrom(current);
    }

    @Override
    public Collection<Content> extract(Object current, @Nullable Field holdingField, IntrospectionContext<?> context, IntrospectionSettings settings) {
        Iterable<?> it = (Iterable<?>) current;
        Stream<?> source = StreamSupport.stream(it.spliterator(), false);
        return source.map(value -> new Content(value, holdingField))
                .toList();
    }
}
