package systems.helius.reflet.reflection;

import jakarta.annotation.Nullable;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.*;

public sealed class ClassInspector permits CachingClassInspector {
    protected final LookupManager lookupManager;

    public ClassInspector() {
        this.lookupManager = new LookupManager();
    }

    public ClassInspector(LookupManager lookupManager) {
        this.lookupManager = lookupManager;
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
     * the class of each superclass of the target class.
     */
    public Map<Class<?>, List<Field>> getAllFieldsHierarchical(Class<?> clazz) {
        var fields = new LinkedHashMap<Class<?>, List<Field>>();
        fields.put(clazz, List.of(clazz.getDeclaredFields()));
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null
                && !superClass.equals(Object.class)
                && !superClass.equals(Enum.class)) {
            fields.putAll(getAllFieldsHierarchical(superClass));
        }
        return fields;
    }

    /**
     * Get all the fields that are present in members of a given class.
     * Recursively checks up into the class tree of clazz to accumulate members.
     *
     * @param clazz to analyze
     * @return all the fields that members of clazz have.
     */
    public List<Field> getAllFieldsFlat(Class<?> clazz) {
        Map<Class<?>, List<Field>> hierarchical = getAllFieldsHierarchical(clazz);
        int reserve = hierarchical.values().stream().mapToInt(List::size).sum();
        ArrayList<Field> buffer = new ArrayList<>(reserve);
        for (List<Field> fields : hierarchical.values()) {
            buffer.addAll(fields);
        }
        return buffer;
    }

    /**
     * Get all the fields and their private handle that are present in members of a given class.
     * @param clazz to analyze
     * @param context the context of the lookup
     * @return a map where the key is the field and the value its access handle.
     * @throws IllegalAccessException if the context is not allowed to access the field
     */
    public Map<Field, VarHandle> getAllFieldsHandles(Class<?> clazz, MethodHandles.Lookup context) throws IllegalAccessException {
        Map<Field, VarHandle> handles = new LinkedHashMap<>();
        MethodHandles.Lookup privilegedLookup = context;
        for (Map.Entry<Class<?>, List<Field>> fieldsByClass :  getAllFieldsHierarchical(clazz).entrySet()) {
            if (context.lookupClass() != fieldsByClass.getKey()) {
                // This grants access to the private fields within superclasses
                try {
                    privilegedLookup = lookupManager.getPrivilegedLookup(fieldsByClass.getKey(), context, privilegedLookup);
                } catch (LoookupAcquisitionException e) {
                    throw new IllegalAccessException("Couldn't get private access to the class: " + fieldsByClass.getKey().getCanonicalName() + ". " + e.getMessage());
                }
            }
            for (Field field : fieldsByClass.getValue()) {
                handles.put(field, privilegedLookup.unreflectVarHandle(field));
            }
        }
        return handles;
    }

    /**
     *
     * @param targetType the sought type
     * @param value the object being checked
     * @param originalType Because of implicit casting rules in the Java Language, primitives are implicitly converted
     *                     to their wrapper type when passed to a method that takes an Object. Passing the original
     *                     type of the field that held the value allows us to deduce the correct true type of the value.
     * @return true if the real type of the value is the target type or is a child of it.
     */
    public static boolean evaluateTypingMatch(Class<?> targetType, Object value, @Nullable Class<?> originalType) {
        if (originalType != null) {
            if (originalType == Void.class)
                return true;

            if (originalType.isPrimitive()) {
                return targetType == originalType;
            }
        }
        return targetType.isAssignableFrom(value.getClass());
    }
}
