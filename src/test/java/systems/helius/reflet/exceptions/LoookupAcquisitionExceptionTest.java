package systems.helius.reflet.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoookupAcquisitionExceptionTest {

    @Test
    void givenMessage_WhenCreatingException_ThenMessageIsSet() {
        String message = "Test exception message";
        LoookupAcquisitionException exception = new LoookupAcquisitionException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void givenMessageAndCause_WhenCreatingException_ThenMessageAndCauseAreSet() {
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Cause of the exception");
        LoookupAcquisitionException exception = new LoookupAcquisitionException(message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}