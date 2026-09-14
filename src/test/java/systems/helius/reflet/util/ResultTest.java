package systems.helius.reflet.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    @Test
    void GivenOkResult_WhenAccessingValue_ThenReturnsValue() {
        Result<String, Exception> result = Result.ok("Success");
        assertTrue(result.isOk());
        assertEquals("Success", result.value().orElseThrow());
        assertFalse(result.error().isPresent());
    }

    @Test
    void GivenErrResult_WhenAccessingError_ThenReturnsError() {
        Result<String, Object> result = Result.err(new IllegalArgumentException());
        assertTrue(result.isErr());
        assertEquals(IllegalArgumentException.class, result.error().orElseThrow().getClass());
        assertFalse(result.value().isPresent());
    }

    @Test
    void GivenTwoEqualResults_WhenComparing_ThenTheyAreEqual() {
        Result<String, Exception> result1 = Result.ok("Success");
        Result<String, Exception> result2 = Result.ok("Success");
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void GivenTwoDifferentResults_WhenComparing_ThenTheyAreNotEqual() {
        Result<String, Exception> result1 = Result.ok("Success");
        Result<String, Exception> result2 = Result.ok("Failure");
        assertNotEquals(result1, result2);
        assertNotEquals(result1.hashCode(), result2.hashCode());
    }

    @SuppressWarnings("rawtypes")
    @Test
    void GivenResultWithBothValueAndError_WhenCreating_ThenThrowsException() throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get the private constructor of Result class using reflection
        Constructor<Result> constructor = Result.class.getDeclaredConstructor(Object.class, Object.class);
        constructor.setAccessible(true);

        try {
            constructor.newInstance("value", new Exception());
            fail("Expected IllegalArgumentException to be thrown"); //NOSONAR We want to test the cause of the exception, the surface is just a wrapper.
        } catch (InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    void GivenResultWithNeitherValueNorError_WhenCreating_ThenThrowsException() throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get the private constructor of Result class using reflection
        Constructor<Result> constructor = Result.class.getDeclaredConstructor(Object.class, Object.class);
        constructor.setAccessible(true);

        try {
            constructor.newInstance(null, null);
            fail("Expected IllegalArgumentException to be thrown"); //NOSONAR We want to test the cause of the exception, the surface is just a wrapper.
        } catch (InvocationTargetException e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause());
        }
    }
}