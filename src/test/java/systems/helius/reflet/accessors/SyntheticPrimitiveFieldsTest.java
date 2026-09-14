package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link SyntheticPrimitiveFields}.
 */
class SyntheticPrimitiveFieldsTest {

    @Test
    void GivenPrimitiveType_WhenGetSyntheticPrimitiveField_ThenReturnsMatchingField() {
        assertEquals(int.class, SyntheticPrimitiveFields.getSyntheticPrimitiveField(int.class).getType());
    }

    @Test
    void GivenUnsupportedType_WhenGetSyntheticPrimitiveField_ThenThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SyntheticPrimitiveFields.getSyntheticPrimitiveField(String.class));
    }
}
