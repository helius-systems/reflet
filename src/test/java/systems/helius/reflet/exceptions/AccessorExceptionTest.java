package systems.helius.reflet.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessorExceptionTest {

    @Test
    void GivenMessage_WhenConstructed_ThenStoresMessageAndIsNotFatal() {
        AccessorException exception = new AccessorException("boom");

        assertEquals("boom", exception.getMessage());
        assertNull(exception.getCause());
        assertFalse(exception.isFatal());
    }

    @Test
    void GivenMessageAndCause_WhenConstructed_ThenStoresMessageCauseAndIsNotFatal() {
        IllegalStateException cause = new IllegalStateException("broken");
        AccessorException exception = new AccessorException("boom", cause);

        assertEquals("boom", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.isFatal());
    }
}