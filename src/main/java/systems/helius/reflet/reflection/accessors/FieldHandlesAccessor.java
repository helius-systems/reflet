package systems.helius.reflet.reflection.accessors;

import jakarta.annotation.Nullable;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;
import systems.helius.reflet.reflection.*;
import systems.helius.reflet.reflection.LookupManager;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Accessor that uses Fields and VarHandles to access fields of classes directly.
 */
public class FieldHandlesAccessor implements ContentAccessor, ClassInspectorAware<FieldHandlesAccessor> {
    private final ClassInspector classInspector;
    private final LookupManager lookupManager;

    public FieldHandlesAccessor(ClassInspector classInspector, LookupManager lookupManager) {
        this.classInspector = classInspector;
        this.lookupManager = lookupManager;
    }

    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return true;
    }

    @Override
    public Collection<Content> extract(Object current, @Nullable Field holdingField, IntrospectionContext<?> context, IntrospectionSettings settings) throws ChainComponentException {
        Map<Class<?>, List<Field>> fields = classInspector.getAllFieldsHierarchical(current.getClass());
        if (fields.isEmpty()) return Collections.emptyList();

        var result = new ArrayList<Content>();

        MethodHandles.Lookup classLookup = getClassLookup(current, context);
        for (Map.Entry<Class<?>, List<Field>> entry : fields.entrySet()) {
            if (classLookup.lookupClass() != entry.getKey()) {
                // This grants access to the private fields within superclasses
                try {
                    classLookup = lookupManager.getPrivilegedLookup(entry.getKey(), context.rootLookup(), classLookup);
                } catch (LoookupAcquisitionException e) {
                    if (!settings.useSafeAccessCheck()) { // TODO rename this parameter to be positive along "fail if inaccessible"
                        throw new ChainComponentException(e, true);
                    }
                    continue;
                }
            }

            accessFields(current, settings, entry, classLookup, result);
        }
        return result;
    }

    private static void accessFields(Object current, IntrospectionSettings settings, Map.Entry<Class<?>, List<Field>> entry, MethodHandles.Lookup classLookup, ArrayList<Content> result) throws ChainComponentException {
        for (Field field : entry.getValue()) {
            try {
                if (Modifier.isStatic(field.getModifiers()))
                    continue;

                Object value = classLookup.unreflectVarHandle(field).get(current);
                if (value != null) {
                    result.add(new Content(value, field));
                }
            } catch (IllegalAccessException e) {
                if (!settings.useSafeAccessCheck()) {
                    var traced = new TracedAccessException("Couldn't read the value of the field: " + field
                            + ". This should be impossible. " +
                            "Please file an issue at https://github.com/SBeausoleil/reflet/issues" +
                            " describing how this happened.", e);
                    throw new ChainComponentException(traced, true);
                }
            }
        }
    }

    private MethodHandles.Lookup getClassLookup(Object current, IntrospectionContext<?> context) throws ChainComponentException {
        try {
            return lookupManager.getPrivilegedLookup(current.getClass(), context.rootLookup(),  MethodHandles.lookup());
        } catch (LoookupAcquisitionException e) {
            throw new ChainComponentException(e, true);
        }
    }

    @Override
    public FieldHandlesAccessor replaceClassInspector(ClassInspector classInspector) {
        return new FieldHandlesAccessor(classInspector, this.lookupManager);
    }
}
