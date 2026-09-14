package systems.helius.reflet;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.fixtures.Foo;

import static org.junit.jupiter.api.Assertions.*;

class CachingClassInspectorTest {

    CachingClassInspector cachingClassInspector = new CachingClassInspector();

    @Test
    void GivenClass_WhenCallTwiceGetAllFieldsHierarchical_ThenReturnSameInstance() {
        Class<?> clazz = Foo.class;

        var firstCall = cachingClassInspector.getAllFieldsHierarchical(clazz);
        var secondCall = cachingClassInspector.getAllFieldsHierarchical(clazz);

        assertSame(firstCall, secondCall, "Expected the same instance to be returned for hierarchical fields");
    }

    @Test
    void GivenClass_WhenCallGetAllFieldsFlat_ThenReturnSameInstance() {
        Class<?> clazz = Foo.class;

        var firstCall = cachingClassInspector.getAllFieldsFlat(clazz);
        var secondCall = cachingClassInspector.getAllFieldsFlat(clazz);

        assertSame(firstCall, secondCall, "Expected the same instance to be returned for flat fields");
    }

    @Test
    void GivenCached_WhenModifyReturnedMapOfGetAllFieldsHierarchical_ThenThrow() {
        Class<?> clazz = Foo.class;

        var fieldsMap = cachingClassInspector.getAllFieldsHierarchical(clazz);

        assertThrows(UnsupportedOperationException.class, () -> fieldsMap.put(Object.class, null), "Expected UnsupportedOperationException when modifying the returned map");
    }

    @Test
    void GivenCached_WhenModifyReturnedListOfGetAllFieldsFlat_ThenThrow() {
        Class<?> clazz = Foo.class;

        var fields = cachingClassInspector.getAllFieldsFlat(clazz);

        assertThrows(UnsupportedOperationException.class, () -> fields.add(null), "Expected UnsupportedOperationException when modifying the returned list");
    }
}