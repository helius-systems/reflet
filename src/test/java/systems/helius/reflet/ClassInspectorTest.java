package systems.helius.reflet;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.fixtures.BarEnum;
import systems.helius.reflet.fixtures.ChildClassA;
import systems.helius.reflet.fixtures.Foo;
import systems.helius.reflet.fixtures.Superclass;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClassInspectorTest {

    protected ClassInspector getInstance() {
        return new ClassInspector();
    }

    @Test
    void getAllFieldsHandles() throws IllegalAccessException, NoSuchFieldException {
        Foo foo = new Foo(5, "Hello");
        Map<Field, VarHandle> handles = getInstance().getAllFieldsHandles(foo.getClass(), MethodHandles.lookup());
        assertEquals(Foo.class.getDeclaredFields().length, handles.size());

        final int A_NEW_VALUE = 1;
        handles.get(Foo.class.getDeclaredField("a")).set(foo, A_NEW_VALUE);
        assertEquals(A_NEW_VALUE, foo.getA());

        final String B_NEW_VALUE = "World";
        VarHandle bHandle = handles.get(Foo.class.getDeclaredField("b"));
        bHandle.set(foo, B_NEW_VALUE);
        assertEquals(B_NEW_VALUE, bHandle.get(foo));
        assertTrue(foo.toString().contains(B_NEW_VALUE));
    }

    @Test
    void getAllFieldsFlat() {
        List<Field> fields = getInstance().getAllFieldsFlat(ChildClassA.class);
        var expectedFields = new LinkedList<Field>();
        expectedFields.addAll(List.of(ChildClassA.class.getDeclaredFields()));
        expectedFields.addAll(List.of(Superclass.class.getDeclaredFields()));
        assertEquals(expectedFields, fields);
    }

    @Test
    void GivenInaccessibleClass_WhenGetAllFieldsHandles_ThenThrowsIllegalAccessException() {
        ClassInspector inspector = getInstance();
        assertThrows(IllegalAccessException.class, () ->
            inspector.getAllFieldsHandles(String.class, MethodHandles.lookup())
        );
    }

    @Test
    void GivenVoidOriginalClass_WhenEvaluateTypingMatchWithVoidTargetClass_ThenReturnsTrue() {
        assertTrue(ClassInspector.evaluateTypingMatch(void.class, null, void.class));
    }

    @Test
    void GivenVoidWrapperOriginalType_WhenEvaluateTypingMatch_ThenReturnsTrue() {
        assertTrue(ClassInspector.evaluateTypingMatch(Object.class, new Object(), Void.class));
    }

    @Test
    void GivenEnum_WhenGetAllFieldsHierarchical_ThenStopsAtEnumSuperclass() {
        Map<Class<?>, List<Field>> fields = getInstance().getAllFieldsHierarchical(BarEnum.class);
        assertTrue(fields.containsKey(BarEnum.class));
        assertFalse(fields.containsKey(Enum.class));
    }

    @Test
    void GivenTypeWithoutSuperclass_WhenGetAllFieldsHierarchical_ThenContainsOnlyThatType() {
        Map<Class<?>, List<Field>> fields = getInstance().getAllFieldsHierarchical(Runnable.class);
        assertEquals(Set.of(Runnable.class), fields.keySet());
    }

    @Test
    void GivenLookupClassMatchingTarget_WhenGetAllFieldsHandles_ThenUsesProvidedLookup() throws IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Foo.class, MethodHandles.lookup());
        Map<Field, VarHandle> handles = getInstance().getAllFieldsHandles(Foo.class, lookup);
        assertEquals(Foo.class.getDeclaredFields().length, handles.size());
    }

    @Test
    void GivenClass_WhenModifyReturnedMapOfGetAllFieldsHierarchical_ThenThrow() throws NoSuchFieldException {
        var fieldsMap = getInstance().getAllFieldsHierarchical(ChildClassA.class);

        // Some random valid Field object:
        Field someField = Foo.class.getDeclaredField("a");
        var asList = List.of(someField);
        assertThrows(UnsupportedOperationException.class, () -> fieldsMap.put(Foo.class, asList),
                "Expected the returned map to be unmodifiable");
    }

    @Test
    void GivenClass_WhenModifyListWithinReturnedMapOfGetAllFieldsHierarchical_ThenThrow() throws NoSuchFieldException {
        var fieldsMap = getInstance().getAllFieldsHierarchical(ChildClassA.class);
        List<Field> fields = fieldsMap.get(ChildClassA.class);

        // Some random valid Field object:
        Field someField = Foo.class.getDeclaredField("a");
        assertThrows(UnsupportedOperationException.class, () -> fields.add(someField),
                "Expected the lists within the returned map to be unmodifiable");
    }

    @Test
    void GivenClass_WhenModifyReturnedListOfGetAllFieldsFlat_ThenThrow() throws NoSuchFieldException {
        List<Field> fields = getInstance().getAllFieldsFlat(ChildClassA.class);

        Field someField = Foo.class.getDeclaredField("a");
        assertThrows(UnsupportedOperationException.class, () -> fields.add(someField),
                "Expected the returned list to be unmodifiable");
    }
}