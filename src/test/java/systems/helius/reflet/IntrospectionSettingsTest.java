package systems.helius.reflet;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.accessors.AccessorsChain;
import systems.helius.reflet.accessors.Content;
import systems.helius.reflet.accessors.ContentAccessor;
import systems.helius.reflet.exceptions.ExceptionContext;
import systems.helius.reflet.exceptions.ExceptionResolution;
import systems.helius.reflet.exceptions.IntrospectionFailureHandler;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class IntrospectionSettingsTest {

    @Test
    void GivenDefaultSettings_WhenRead_ThenTheyMatchTheDocumentedDefaults() {
        // Given
        IntrospectionSettings settings = new IntrospectionSettings();

        // When
        ExceptionResolution resolution = settings.getExceptionHandler().handle(
                new ExceptionContext(new IllegalStateException("boom"), new Object(), null, String.class,
                        ExceptionContext.Origin.ACCESSOR));

        // Then
        assertEquals(AccessDenialPolicy.SKIP, settings.getAccessDenialPolicy());
        assertInstanceOf(ExceptionResolution.Propagate.class, resolution);
        assertTrue(settings.isEnterTargetType());
        assertEquals(Integer.MAX_VALUE, settings.getMaxDepth());
        assertNotNull(settings.getContentAccessor());
        assertInstanceOf(AccessorsChain.class, settings.getContentAccessor());
    }

    @Test
    void GivenCustomBuilderValues_WhenBuilt_ThenSettingsExposeTheDocumentedValues() {
        // Given
        ContentAccessor customAccessor = new StubAccessor();
        IntrospectionFailureHandler customHandler = context -> ExceptionResolution.skip();

        // When
        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.FAIL)
                .withExceptionHandler(customHandler)
                .withEnterTargetType(false)
                .withMaxDepth(3)
                .withContentAccessor(customAccessor)
                .build();

        // Then
        assertEquals(AccessDenialPolicy.FAIL, settings.getAccessDenialPolicy());
        assertSame(customHandler, settings.getExceptionHandler());
        assertFalse(settings.isEnterTargetType());
        assertEquals(3, settings.getMaxDepth());
        assertSame(customAccessor, settings.getContentAccessor());
    }

    @Test
    void GivenSettings_WhenToBuilder_ThenItCopiesTheDocumentedValues() {
        // Given
        ContentAccessor customAccessor = new StubAccessor();
        IntrospectionFailureHandler customHandler = context -> ExceptionResolution.skip();
        IntrospectionSettings original = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.FAIL)
                .withExceptionHandler(customHandler)
                .withEnterTargetType(false)
                .withMaxDepth(11)
                .withContentAccessor(customAccessor)
                .build();

        // When
        IntrospectionSettings copy = original.toBuilder().build();

        // Then
        assertEquals(original.getAccessDenialPolicy(), copy.getAccessDenialPolicy());
        assertSame(original.getExceptionHandler(), copy.getExceptionHandler());
        assertEquals(original.isEnterTargetType(), copy.isEnterTargetType());
        assertEquals(original.getMaxDepth(), copy.getMaxDepth());
        assertSame(original.getContentAccessor(), copy.getContentAccessor());
    }

    @SuppressWarnings("squid:S5778") // False positive: rebuilding the builder each time is intentional to test each argument separately
    @Test
    void GivenNullBuilderArguments_WhenConfigured_ThenTheyAreRejected() {
        assertThrows(NullPointerException.class,
                () -> IntrospectionSettings.builder().withAccessDenialPolicy(null));
        assertThrows(NullPointerException.class,
                () -> IntrospectionSettings.builder().withExceptionHandler(null));
        assertThrows(NullPointerException.class,
                () -> IntrospectionSettings.builder().withContentAccessor(null));
    }

    @Test
    void GivenNegativeMaxDepth_WhenBuild_ThenItRejectsTheConfiguration() {
        // Given
        IntrospectionSettings.Builder builder = IntrospectionSettings.builder().withMaxDepth(-1);

        // When / Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("maxDepth must be >= 0", exception.getMessage());
    }

    private static final class StubAccessor implements ContentAccessor {
        @Override
        public boolean accepts(Class<?> current, Field holdingField) {
            return true;
        }

        @Override
        public Collection<Content> extract(Object current, Field holdingField, IntrospectionContext<?> context,
                                          IntrospectionSettings settings) {
            return Collections.emptyList();
        }
    }
}