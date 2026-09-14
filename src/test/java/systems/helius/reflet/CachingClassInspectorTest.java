package systems.helius.reflet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.helius.reflet.fixtures.ChildClassA;
import systems.helius.reflet.fixtures.DeepHierarchy;
import systems.helius.reflet.fixtures.Foo;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CachingClassInspectorTest extends ClassInspectorTest {

    CachingClassInspector cachingClassInspector;

    @BeforeEach
    void setUp() {
        cachingClassInspector = new CachingClassInspector();
    }

    @Override
    protected ClassInspector getInstance() {
        return cachingClassInspector;
    }

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

        var freshlyComputed = cachingClassInspector.getAllFieldsHierarchical(clazz);
        var cached = cachingClassInspector.getAllFieldsHierarchical(clazz);

        assertThrows(UnsupportedOperationException.class, () -> freshlyComputed.put(Object.class, null), "Expected UnsupportedOperationException when modifying the returned map");
        assertThrows(UnsupportedOperationException.class, () -> cached.put(Object.class, null), "Expected UnsupportedOperationException when modifying the returned map");
    }

    @Test
    void GivenCached_WhenModifyReturnedListOfGetAllFieldsFlat_ThenThrow() throws NoSuchFieldException {
        Class<?> clazz = Foo.class;

        var freshlyComputed = cachingClassInspector.getAllFieldsFlat(clazz);
        var cachedFields = cachingClassInspector.getAllFieldsFlat(clazz);

        Field someField = Foo.class.getDeclaredField("a");
        assertThrows(UnsupportedOperationException.class, () -> freshlyComputed.add(someField), "Expected UnsupportedOperationException when modifying the returned list");
        assertThrows(UnsupportedOperationException.class, () -> cachedFields.add(someField), "Expected UnsupportedOperationException when modifying the returned list");
    }

    @Test
    void GivenCached_WhenModifyListWithinReturnedMapOfGetAllFieldsHierarchical_ThenThrow() throws NoSuchFieldException {
        Class<?> clazz = ChildClassA.class;

        var freshlyComputed = cachingClassInspector.getAllFieldsHierarchical(clazz);
        var cached = cachingClassInspector.getAllFieldsHierarchical(clazz);

        Field someField = Foo.class.getDeclaredField("a");
        assertThrows(UnsupportedOperationException.class, () -> freshlyComputed.get(clazz).add(someField), "Expected the lists within the returned map to be unmodifiable"); //NOSONAR
        assertThrows(UnsupportedOperationException.class, () -> cached.get(clazz).add(someField), "Expected the lists within the returned map to be unmodifiable"); //NOSONAR
    }

    @Test
    void GivenDeepHierarchy_WhenCallGetAllFieldsHierarchical_ThenReturnCorrectHierarchy() {
        Class<?> clazz = DeepHierarchy.L1.class;

        var fieldsMap = cachingClassInspector.getAllFieldsHierarchical(clazz);

        assertTrue(fieldsMap.containsKey(clazz), "Expected the map to contain the leaf class");
        assertTrue(fieldsMap.containsKey(DeepHierarchy.L20.class), "Expected the map to contain the top-most superclass");
    }
}