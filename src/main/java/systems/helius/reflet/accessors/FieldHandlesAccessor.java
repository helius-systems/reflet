package systems.helius.reflet.accessors;

import jakarta.annotation.Nullable;
import systems.helius.reflet.*;
import systems.helius.reflet.exceptions.AccessorException;
import systems.helius.reflet.exceptions.TracedAccessException;
import systems.helius.reflet.util.Result;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

/**
 * Accessor that uses Fields and VarHandles to access fields of classes directly.
 */
public class FieldHandlesAccessor implements ContentAccessor {
    private final ClassInspector classInspector;
    private final LookupManager lookupManager;

    /**
     * Creates an accessor that reads fields using method-handle lookups.
     *
     * @param classInspector inspector used to enumerate fields.
     * @param lookupManager  manager used to acquire privileged lookups.
     */
    public FieldHandlesAccessor(ClassInspector classInspector, LookupManager lookupManager) {
        this.classInspector = classInspector;
        this.lookupManager = lookupManager;
    }

    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return true;
    }

    @Override
    public Collection<Content> extract(Object current, @Nullable Field holdingField, IntrospectionContext<?> context, IntrospectionSettings settings) throws AccessorException {
        Map<Class<?>, List<Field>> fields = classInspector.getAllFieldsHierarchical(current.getClass());
        if (fields.isEmpty()) return Collections.emptyList();

        var result = new ArrayList<Content>();

        MethodHandles.Lookup classLookup = getClassLookup(current, context, settings);
        if (classLookup == null) {
            return result;
        }
        for (Map.Entry<Class<?>, List<Field>> entry : fields.entrySet()) {
            if (classLookup.lookupClass() != entry.getKey()) {
                // This grants access to the private fields within superclasses
                Result<MethodHandles.Lookup, Supplier<String>> lookupResult =
                        lookupManager.getPrivilegedLookup(entry.getKey(), context.rootLookup(), classLookup);
                if (lookupResult.isErr()) {
                    if (settings.getAccessDenialPolicy() == AccessDenialPolicy.FAIL) {
                        throw new AccessorException("Failed to acquire privileged lookup for class: " + entry.getKey()
                                + ". " + lookupResult.error().orElseThrow().get());
                    }
                    continue;
                }
                classLookup = lookupResult.value().orElseThrow();
            }

            accessFields(current, settings, entry, classLookup, result);
        }
        return result;
    }

    private static void accessFields(Object current, IntrospectionSettings settings, Map.Entry<Class<?>, List<Field>> entry, MethodHandles.Lookup classLookup, ArrayList<Content> result) throws AccessorException {
        for (Field field : entry.getValue()) {
            try {
                if (Modifier.isStatic(field.getModifiers()))
                    continue;

                Object value = classLookup.unreflectVarHandle(field).get(current);
                if (value != null) {
                    result.add(new Content(value, field));
                }
            } catch (IllegalAccessException e) {
                throw new AccessorException("Couldn't read the value of the field: " + field
                        + " AFTER obtaining a privileged lookup for it. This should be impossible." +
                        " Please file an issue at https://github.com/helius-systems/reflet/issues" +
                        " describing how this happened.", e);
            }
        }
    }

    /**
     * Resolves the root class lookup for the current object.
     *
     * @param current  the object being inspected.
     * @param context  the current introspection context.
     * @param settings settings of the current search.
     * @return the lookup, or {@code null} when access is denied and denial should be skipped.
     * @throws AccessorException if access is denied and denial should fail the search.
     */
    @Nullable
    private MethodHandles.Lookup getClassLookup(Object current,
                                                IntrospectionContext<?> context,
                                                IntrospectionSettings settings) throws AccessorException {
        Result<MethodHandles.Lookup, Supplier<String>> lookupResult =
                lookupManager.getPrivilegedLookup(current.getClass(), context.rootLookup(), MethodHandles.lookup());
        if (lookupResult.isErr()) {
            if (settings.getAccessDenialPolicy() == AccessDenialPolicy.FAIL) {
                throw AccessorException.fatal("Failed to acquire privileged lookup for class: " + current.getClass()
                        + ". " + lookupResult.error().orElseThrow().get(), null);
            }
            return null;
        }
        return lookupResult.value().orElseThrow();
    }
}
