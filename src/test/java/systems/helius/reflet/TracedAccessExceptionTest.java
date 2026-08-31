package systems.helius.reflet;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TracedAccessExceptionTest {

    @Test
    void GivenRootAndFieldTrace_WhenBuildPath_ThenIncludesRootAndFieldSteps() throws NoSuchFieldException {
        TestRoot root = new TestRoot();
        Field firstField = TestRoot.class.getDeclaredField("first");
        Field secondField = TestRoot.class.getDeclaredField("second");

        TracedAccessException exception = new TracedAccessException("boom");
        exception.setRoot(root);
        exception.addStep(firstField);
        exception.addStep(secondField);

        String path = exception.buildPath();

        assertTrue(path.contains("boom"));
        assertTrue(path.contains("Path leading to issue: "));
        assertTrue(path.contains("[root]: "));
        assertTrue(path.contains(TestRoot.class.getCanonicalName()));
        assertTrue(path.contains(root.toString()));
        assertTrue(path.contains(firstField.toString()));
        assertTrue(path.contains(secondField.toString()));
        assertTrue(path.indexOf(secondField.toString()) < path.indexOf(firstField.toString()));
    }

    @Test
    void GivenNullStep_WhenAddStep_ThenDoesNothing() {
        TracedAccessException exception = new TracedAccessException("boom");

        exception.addStep(null);

        assertEquals("boomPath leading to issue: ", exception.buildPath());
    }

    @Test
    void GivenRoot_WhenSetRootAndGetRoot_ThenReturnsSameRoot() {
        Object root = new Object();
        TracedAccessException exception = new TracedAccessException("boom");

        exception.setRoot(root);

        assertSame(root, exception.getRoot());
    }

    // NOSONAR needed for detection
    private static final class TestRoot {
        private String first;
        private String second;
    }
}