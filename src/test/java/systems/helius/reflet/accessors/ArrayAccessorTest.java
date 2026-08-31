package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.fixtures.Foo;

import static org.junit.jupiter.api.Assertions.*;

class ArrayAccessorTest {

    ArrayAccessor arrayAccessor = new ArrayAccessor();

    @Test
    void GivenArray_WhenAccepts_ThenReturnTrue() {
        assertTrue(arrayAccessor.accepts(Foo[].class, null));
    }

    @Test
    void GivenPrimitiveArray_WhenAccepts_ThenReturnTrue() {
        assertTrue(arrayAccessor.accepts(int[].class, null));
    }

    @Test
    void GivenObjectNotArray_WhenAccepts_ThenReturnFalse() {
        assertFalse(arrayAccessor.accepts(Foo.class, null));
    }

    @Test
    void GivenPrimitive_WhenAccepts_ThenReturnFalse() {
        assertFalse(arrayAccessor.accepts(int.class, null));
    }
}