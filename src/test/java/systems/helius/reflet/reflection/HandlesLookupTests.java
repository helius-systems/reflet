package systems.helius.reflet.reflection;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.fixtures.ChildClassA;
import systems.helius.reflet.fixtures.Foo;
import systems.helius.reflet.fixtures.Superclass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodHandles.Lookup;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HandlesLookupTests {
    @Test
    public void testViaFieldName() throws Throwable {
        Lookup lookup = MethodHandles.privateLookupIn(Foo.class, MethodHandles.lookup());
        MethodHandle fieldNameAccess = lookup.findGetter(Foo.class, "a", int.class);
        var foo = new Foo(5, "Hello world");
        int result = (int) fieldNameAccess.invoke(foo);
        assertEquals(5, result);
    }

    @Test
    public void testViaMethodName_reflection() throws Throwable {
        Lookup lookup = MethodHandles.lookup().in(Foo.class);
        Method getterMethod = lookup.lookupClass().getMethod("getA");
        var foo = new Foo(5, "Hello world");
        int result = (int) getterMethod.invoke(foo);
        assertEquals(5, result);
    }

    @Test
    public void testViaMethodName_virtualLookup() throws Throwable {
        Lookup lookup = MethodHandles.lookup().in(Foo.class);
        MethodType methodType = MethodType.methodType(int.class);
        MethodHandle getterMethod = lookup.findVirtual(lookup.lookupClass(), "getA", methodType);
        var foo = new Foo(5, "Hello world");
        int result = (int) getterMethod.invoke(foo);
        assertEquals(5, result);
    }

    @Test
    public void testViaMethodName_methodHandle() throws Throwable {
        Lookup lookup = MethodHandles.lookup().in(Foo.class);
        MethodType methodType = MethodType.methodType(int.class);
        MethodHandle getterMethod = lookup.findVirtual(lookup.lookupClass(), "getA", methodType);
        var foo = new Foo(5, "Hello world");
        int result = (int) getterMethod.invokeExact(foo);
        assertEquals(5, result);
    }

    @Test
    public void testViaMethodName_methodHandle_invokeExactWithInheritance() throws Throwable {
        Lookup lookup = MethodHandles.lookup().in(ChildClassA.class);
        MethodType methodType = MethodType.methodType(int.class);
        MethodHandle getterMethod = lookup.findVirtual(lookup.lookupClass(), "getSuperclassField", methodType);
        var foo = new ChildClassA(5, "Foo");
        /* invokeExact does not work when called upon the child of the declaring class!
         * Otherwise, use invoke() if you are not sure of the true class of the instance
         * that the MethodHandle will use for invocation.
         */
        int result = (int) getterMethod.invokeExact(foo);
        assertEquals(5, result);
    }

    @Test
    public void testViaVarHandle() throws Throwable {
        Lookup lookup = MethodHandles.privateLookupIn(Superclass.class, MethodHandles.lookup());
        VarHandle varHandle = lookup.findVarHandle(ChildClassA.class, "superclassField", int.class);
        var foo = new ChildClassA(5, "Foo");
        int result = (int) varHandle.get(foo);
        assertEquals(5, result);
    }

    @Test
    public void testPrivateAccess_withVirtualGetter() throws Throwable {
        Lookup origin = MethodHandles.lookup();
        Lookup lookup = origin.in(ChildClassA.class);
        var foo = new ChildClassA(5, "Foo");
        // Fails: private access
        //String name = (String) lookup.findGetter(ChildClassA.class, "name", String.class).invokeExact(foo);
        //assertEquals("Foo", name);

        // Fails: private access
        //Field field = foo.getClass().getDeclaredField("name");
        //lookup.unreflectGetter(field);
        //assertEquals("Foo", field.get(foo));

        Field field = foo.getClass().getDeclaredField("name");
        field.setAccessible(true);
        lookup.unreflectGetter(field);
        assertEquals("Foo", field.get(foo));

        // Fails: private access
        //lookup.findVarHandle(ChildClassA.class, "name", String.class);
        //assertEquals("Foo", field.get(foo));

        // Fails when getting lookup: caller does not have PRIVATE and MODULE lookup mode
        //Lookup privateLookup = MethodHandles.privateLookupIn(ChildClassA.class, lookup);
        //privateLookup.findVarHandle(ChildClassA.class, "name", String.class);
        //assertEquals("Foo", field.get(foo));

        Lookup privateLookup = MethodHandles.privateLookupIn(ChildClassA.class, origin);
        VarHandle handle = privateLookup.findVarHandle(ChildClassA.class, "name", String.class);
        assertEquals("Foo", handle.get(foo));
    }

    @Test
    public void testAccessToInheritedPrivateFields() throws NoSuchFieldException, IllegalAccessException {
        var child = new ChildClassA(5, "Foo");

        Lookup privateLookup = MethodHandles.privateLookupIn(Superclass.class, MethodHandles.lookup());
        VarHandle handle = privateLookup.findVarHandle(ChildClassA.class, "superclassField", int.class);
        assertEquals(5, handle.get(child));
    }
}
