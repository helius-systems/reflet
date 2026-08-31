package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.CachingClassInspector;
import systems.helius.reflet.ClassInspector;
import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AccessorsChain} and its builder.
 */
class AccessorsChainTest {

    /**
     * Verifies that the default builder wires the built-in accessors in the expected order.
     */
    @Test
    void GivenDefaults_WhenBuild_ThenIncludesBuiltInAccessorsInOrder() {
        AccessorsChain chain = AccessorsChain.builder(true).build();

        assertEquals(
                List.of(ArrayAccessor.class, IterativeAccessor.class, IterativeMapAccessor.class, FieldHandlesAccessor.class),
                accessorTypes(chain)
        );
    }

    /**
     * Verifies that builder mutation methods affect the final chain as expected.
     */
    @Test
    void GivenCustomConfiguration_WhenApplyingBuilderMethods_ThenBuildReflectsChainMutations() {
        AccessorsChain.Builder builder = AccessorsChain.builder(false);

        assertSame(builder, builder.addLast(new BetaAccessor()));
        assertSame(builder, builder.addFirst(new AlphaAccessor()));
        assertSame(builder, builder.insertAfter(new GammaAccessor(), AlphaAccessor.class));
        assertSame(builder, builder.insertBefore(new DeltaAccessor(), BetaAccessor.class));
        assertSame(builder, builder.replace(BetaAccessor.class, new EpsilonAccessor()));
        assertSame(builder, builder.replaceOrAddAtEnd(ZetaAccessor.class, new ZetaAccessor()));
        assertTrue(builder.remove(GammaAccessor.class));

        AccessorsChain chain = builder.build();

        assertEquals(
                List.of(AlphaAccessor.class, DeltaAccessor.class, EpsilonAccessor.class, ZetaAccessor.class),
                accessorTypes(chain)
        );
    }

    /**
     * Verifies that disabling the last resort accessor removes it from the built chain.
     */
    @Test
    void GivenDefaults_WhenDisablingLastResortAccessor_ThenBuildOmitsFieldHandlesAccessor() {
        AccessorsChain chain = AccessorsChain.builder(true)
                .enableLastResortAccessor(false)
                .build();

        assertFalse(accessorTypes(chain).contains(FieldHandlesAccessor.class));
    }

    /**
     * Verifies that calling setClassInspector updates every class-inspector-aware accessor in the builder.
     */
    @Test
    void GivenClassInspectorAwareAccessors_WhenSetClassInspector_ThenAllAccessorsReceiveReplacementInspector() {
        ClassInspector originalInspector = new CachingClassInspector();
        ClassInspector replacementInspector = new ClassInspector();
        InspectableAccessor firstAccessor = new InspectableAccessor("first", originalInspector);
        InspectableAccessor secondAccessor = new InspectableAccessor("second", originalInspector);

        AccessorsChain chain = AccessorsChain.builder(true)
                .addFirst(firstAccessor)
                .addLast(secondAccessor)
                .setClassInspector(replacementInspector)
                .build();

        List<ContentAccessor> accessors = accessorChain(chain);

        InspectableAccessor builtFirst = (InspectableAccessor) accessors.get(0);
        InspectableAccessor builtSecond = (InspectableAccessor) accessors.get(4);
        FieldHandlesAccessor builtLastResort = (FieldHandlesAccessor) accessors.get(5);

        assertAll(
                () -> assertNotSame(firstAccessor, builtFirst),
                () -> assertNotSame(secondAccessor, builtSecond),
                () -> assertSame(replacementInspector, builtFirst.classInspector()),
                () -> assertSame(replacementInspector, builtSecond.classInspector()),
                () -> assertSame(replacementInspector, fieldInspector(builtLastResort))
        );
    }

    /**
     * Reads the accessor classes from the built chain.
     */
    private static List<Class<?>> accessorTypes(AccessorsChain chain) {
        List<Class<?>> types = new java.util.ArrayList<>();
        for (ContentAccessor accessor : accessorChain(chain)) {
            types.add(accessor.getClass());
        }
        return types;
    }

    /**
     * Reads the private chain list from {@link AccessorsChain}.
     */
    @SuppressWarnings("unchecked")
    private static List<ContentAccessor> accessorChain(AccessorsChain chain) {
        try {
            Field chainField = AccessorsChain.class.getDeclaredField("chain");
            chainField.setAccessible(true);
            return (List<ContentAccessor>) chainField.get(chain);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect AccessorsChain", e);
        }
    }

    /**
     * Reads the private class inspector field from {@link FieldHandlesAccessor}.
     */
    private static ClassInspector fieldInspector(FieldHandlesAccessor accessor) {
        try {
            Field field = FieldHandlesAccessor.class.getDeclaredField("classInspector");
            field.setAccessible(true);
            return (ClassInspector) field.get(accessor);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect FieldHandlesAccessor", e);
        }
    }

    /**
     * No-op accessor used to exercise builder ordering operations.
     */
    private abstract static class NoopAccessor implements ContentAccessor {
        @Override
        public boolean accepts(Class<?> current, java.lang.reflect.Field holdingField) {
            return false;
        }

        @Override
        public Collection<Content> extract(Object current, java.lang.reflect.Field holdingField,
                                           IntrospectionContext<?> context,
                                           IntrospectionSettings settings) {
            return Collections.emptyList();
        }
    }

    private static final class AlphaAccessor extends NoopAccessor {
    }

    private static final class BetaAccessor extends NoopAccessor {
    }

    private static final class GammaAccessor extends NoopAccessor {
    }

    private static final class DeltaAccessor extends NoopAccessor {
    }

    private static final class EpsilonAccessor extends NoopAccessor {
    }

    private static final class ZetaAccessor extends NoopAccessor {
    }

    /**
     * Class-inspector-aware test accessor that returns a replacement instance when updated.
     */
    private static final class InspectableAccessor extends NoopAccessor implements ClassInspectorAware<InspectableAccessor> {
        private final String name;
        private final ClassInspector classInspector;

        private InspectableAccessor(String name, ClassInspector classInspector) {
            this.name = name;
            this.classInspector = classInspector;
        }

        @Override
        public InspectableAccessor replaceClassInspector(ClassInspector classInspector) {
            return new InspectableAccessor(name, classInspector);
        }

        private ClassInspector classInspector() {
            return classInspector;
        }
    }

}