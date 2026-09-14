package systems.helius.reflet.accessors;

import org.jspecify.annotations.Nullable;
import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Accessor specialized for {@link Map} values.
 */
public class IterativeMapAccessor implements ContentAccessor {
    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return Map.class.isAssignableFrom(current);
    }

    @Override
    public Collection<Content> extract(Object current, @Nullable Field holdingField, IntrospectionContext<?> context, IntrospectionSettings settings) {
        Map<?, ?> map = (Map<?, ?>) current;
        var content = new ArrayList<Content>(map.size() * 2);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            content.add(new Content(entry.getKey(), holdingField));
            content.add(new Content(entry.getValue(), holdingField));
        }
        return content;
    }
}
