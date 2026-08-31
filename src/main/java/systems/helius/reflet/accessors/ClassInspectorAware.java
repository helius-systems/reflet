package systems.helius.reflet.accessors;

import systems.helius.reflet.ClassInspector;

/**
 * Classes that implement this interface have a deep-rooted need to be kept in sync with the ClassInspector used by their context.
 * @param <T> the type of the class implementing this interface, used to specify the return
 *          type of the replaceClassInspector method (Curiously Recurring Template Pattern).
 */
public interface ClassInspectorAware<T extends ClassInspectorAware<T>> {
    /**
     * Replaces the ClassInspector used by this accessor.
     *
     * @param classInspector the new class inspector to use
     * @return the instance that whatever uses this object should use. Can be the same instance or a completely new entity.
     */
    T replaceClassInspector(ClassInspector classInspector);
}
