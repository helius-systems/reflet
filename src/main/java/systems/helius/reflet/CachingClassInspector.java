package systems.helius.reflet;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CachingClassInspector extends ClassInspector {
    private final Map<Class<?>, Map<Class<?>, List<Field>>> hierarchyCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> flatCache = new ConcurrentHashMap<>();

    public CachingClassInspector() {
        super();
    }

    public CachingClassInspector(LookupManager lookupManager) {
        super(lookupManager);
    }

    /**
     * Get all the fields that are present in members of a given class.
     * Recursively checks up into the class tree of clazz to accumulate members.
     * If iterated by entry, the first entry is always the clazz argument
     * and the last entry is always the top-most class in its hierarchy.
     * Object is ignored from the hierarchy.
     *
     * @param clazz to analyze
     * @return all the fields that members of clazz have. This is in the form of a map where the key
     * the class of each superclass of the target class. The map itself and its lists are unmodifiable.
     */
    @Override
    public Map<Class<?>, List<Field>> getAllFieldsHierarchical(Class<?> clazz) {
        return hierarchyCache.computeIfAbsent(clazz, c -> {
            Map<Class<?>, List<Field>> raw = super.getAllFieldsHierarchical(c);
            // Copy mutable lists and map to ensure immutability of the returned structure
            Map<Class<?>, List<Field>> temp = new LinkedHashMap<>();
            for (Map.Entry<Class<?>, List<Field>> entry : raw.entrySet()) {
                temp.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return Collections.unmodifiableMap(temp);
        });
    }

    /**
     * Get all the fields that are present in members of a given class.
     * Recursively checks up into the class tree of clazz to accumulate members.
     *
     * @param clazz to analyze
     * @return all the fields that members of clazz have. The returned list is unmodifiable.
     */
    @Override
    public List<Field> getAllFieldsFlat(Class<?> clazz) {
        return flatCache.computeIfAbsent(clazz, c -> {
            List<Field> raw = super.getAllFieldsFlat(c);
            return Collections.unmodifiableList(raw);
        });
    }
}
