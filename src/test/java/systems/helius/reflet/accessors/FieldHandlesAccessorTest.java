package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.ClassInspector;
import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;
import systems.helius.reflet.LookupManager;

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
}