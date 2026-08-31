package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;
import systems.helius.reflet.ClassInspector;
import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;
import systems.helius.reflet.LookupManager;
import systems.helius.reflet.TracedAccessException;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FieldHandlesAccessor}.
 */
class FieldHandlesAccessorTest {

    /**
     * Verifies the accessor accepts any class.
     */
    @Test
    void GivenAnyClass_WhenAccepts_ThenReturnsTrue() {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        assertTrue(accessor.accepts(Object.class, null));
        assertTrue(accessor.accepts(ChildSample.class, null));
        assertTrue(accessor.accepts(int.class, null));
    }

    /**
     * Verifies extraction reads inherited instance fields while ignoring static and null-valued fields.
     */
    @Test
    void GivenInheritedObject_WhenExtract_ThenReturnsOnlyNonNullInstanceFields() throws ChainComponentException {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new LookupManager());
        ChildSample sample = new ChildSample();

        Collection<Content> extracted = accessor.extract(sample, null, newContext(accessor), new IntrospectionSettings());

        assertEquals(2, extracted.size());
        assertEquals(Set.of("child-value", 7), extracted.stream().map(Content::value).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("childValue", "baseNumber"), extracted.stream().map(content -> content.holdingField().getName()).collect(java.util.stream.Collectors.toSet()));
    }

    /**
     * Verifies safe mode skips inaccessible fields rather than failing.
     */
    @Test
    void GivenInaccessibleLookupAndSafeMode_WhenExtract_ThenSkipsFieldWithoutThrowing() throws ChainComponentException {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new PublicOnlyLookupManager());
        Collection<Content> extracted = accessor.extract(new ChildSample(), null, newContext(accessor), new IntrospectionSettings());

        assertTrue(extracted.isEmpty());
    }

    /**
     * Verifies unsafe mode surfaces field access failures as a traced chain exception.
     */
    @Test
    void GivenInaccessibleLookupAndUnsafeMode_WhenExtract_ThenThrowsChainComponentException() {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new PublicOnlyLookupManager());
        IntrospectionSettings unsafeSettings = IntrospectionSettings.builder()
                .withSafeAccessCheck(false)
                .build();

        ChainComponentException exception = assertThrows(
                ChainComponentException.class,
                () -> accessor.extract(new ChildSample(), null, newContext(accessor), unsafeSettings)
        );

        assertTrue(exception.isAllowFallback());
        assertNotNull(exception.getCause());
        assertInstanceOf(TracedAccessException.class, exception.getCause());
    }

    /**
     * Verifies acquisition failures from the initial class lookup are wrapped in a chain exception.
     */
    @Test
    void GivenLookupAcquisitionFailure_WhenExtract_ThenWrapsException() {
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), new AlwaysFailLookupManager());

        ChainComponentException exception = assertThrows(
                ChainComponentException.class,
                () -> accessor.extract(new ChildSample(), null, newContext(accessor), new IntrospectionSettings())
        );

        assertTrue(exception.isAllowFallback());
        assertNotNull(exception.getCause());
        assertInstanceOf(LoookupAcquisitionException.class, exception.getCause());
    }

    /**
     * Verifies replacing the class inspector creates a new accessor instance with the same lookup manager.
     */
    @Test
    void GivenNewClassInspector_WhenReplaceClassInspector_ThenReturnsNewAccessorWithSameLookupManager() throws ReflectiveOperationException {
        LookupManager lookupManager = new LookupManager();
        FieldHandlesAccessor accessor = new FieldHandlesAccessor(new ClassInspector(), lookupManager);
        ClassInspector replacementInspector = new ClassInspector();

        FieldHandlesAccessor replaced = accessor.replaceClassInspector(replacementInspector);

        assertNotSame(accessor, replaced);

        Field classInspectorField = FieldHandlesAccessor.class.getDeclaredField("classInspector");
        classInspectorField.setAccessible(true);
        assertSame(replacementInspector, classInspectorField.get(replaced));

        Field lookupManagerField = FieldHandlesAccessor.class.getDeclaredField("lookupManager");
        lookupManagerField.setAccessible(true);
        assertSame(lookupManager, lookupManagerField.get(replaced));
    }

    /**
     * Builds a minimal introspection context for accessor tests.
     */
    private static IntrospectionContext<Object> newContext(ContentAccessor accessor) {
        return new IntrospectionContext<>(
                Object.class,
                MethodHandles.lookup(),
                new HashSet<>(),
                Collections.newSetFromMap(new IdentityHashMap<>()),
                accessor
        );
    }

    /**
     * Sample base class used to validate hierarchical field extraction.
     */
    private static class BaseSample {
        @SuppressWarnings("unused")
        private final Integer baseNumber = 7;

        @SuppressWarnings("unused")
        private static final int BASE_STATIC = 999;
    }

    /**
     * Sample derived class used to validate static and null filtering.
     */
    private static class ChildSample extends BaseSample {
        @SuppressWarnings("unused")
        private final String childValue = "child-value";

        @SuppressWarnings("unused")
        private final String nullableValue = null;

        @SuppressWarnings("unused")
        private static final String DERIVED_STATIC = "ignored";
    }

    /**
     * Lookup manager that always returns a public lookup, preventing private-field access.
     */
    private static class PublicOnlyLookupManager extends LookupManager {
        @Override
        public MethodHandles.Lookup getPrivilegedLookup(Class<?> target, MethodHandles.Lookup caller, MethodHandles.Lookup... fallbacks) {
            return MethodHandles.publicLookup();
        }
    }

    /**
     * Lookup manager that always fails privileged-lookup acquisition.
     */
    private static class AlwaysFailLookupManager extends LookupManager {
        @Override
        public MethodHandles.Lookup getPrivilegedLookup(Class<?> target, MethodHandles.Lookup caller, MethodHandles.Lookup... fallbacks) throws LoookupAcquisitionException {
            throw new LoookupAcquisitionException("forced failure for tests");
        }
    }
}