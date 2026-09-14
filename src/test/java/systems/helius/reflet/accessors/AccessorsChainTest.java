package systems.helius.reflet.accessors;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;
import systems.helius.reflet.exceptions.AccessorException;
import systems.helius.reflet.exceptions.ExceptionResolution;
import systems.helius.reflet.fixtures.Foo;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    void GivenArrayAccessor_WhenAcceptsArrayType_ThenReturnsTrue() {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new ArrayAccessor())
                .build();

        assertTrue(chain.accepts(Foo[].class, null));
    }

    @Test
    void GivenMapAccessor_WhenAcceptsMapType_ThenReturnsTrue() {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new IterativeMapAccessor())
                .build();

        assertTrue(chain.accepts(Map.class, null));
    }

    @Test
    void GivenArrayAndMapAccessors_WhenAcceptsNonMatchingType_ThenReturnsFalse() {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new ArrayAccessor())
                .addLast(new IterativeMapAccessor())
                .build();

        assertFalse(chain.accepts(Foo.class, null));
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

    @Test
    void GivenBuilder_WhenTogglingIteratorAccessors_ThenChainAddsAndRemovesExpectedAccessors() {
        AccessorsChain.Builder iterableBuilder = AccessorsChain.builder(false);
        iterableBuilder.iterateOverIterables(true);
        assertEquals(List.of(IterativeAccessor.class), accessorTypes(iterableBuilder.build()));
        iterableBuilder.iterateOverIterables(false);
        assertEquals(List.of(), accessorTypes(iterableBuilder.build()));

        AccessorsChain.Builder mapBuilder = AccessorsChain.builder(false);
        mapBuilder.iterateOverMapEntries(true);
        assertEquals(List.of(IterativeMapAccessor.class), accessorTypes(mapBuilder.build()));
        mapBuilder.iterateOverMapEntries(false);
        assertEquals(List.of(), accessorTypes(mapBuilder.build()));

        AccessorsChain.Builder arrayBuilder = AccessorsChain.builder(false);
        arrayBuilder.iterateOverArrays(true);
        assertEquals(List.of(ArrayAccessor.class), accessorTypes(arrayBuilder.build()));
        arrayBuilder.iterateOverArrays(false);
        assertEquals(List.of(), accessorTypes(arrayBuilder.build()));
    }

    @Test
    void GivenBuilder_WhenReplaceOrAddAtEndMatchesExistingAccessor_ThenItReplacesInPlace() {
        AccessorsChain.Builder builder = AccessorsChain.builder(false)
                .addLast(new AlphaAccessor())
                .addLast(new BetaAccessor());

        assertSame(builder, builder.replaceOrAddAtEnd(BetaAccessor.class, new EpsilonAccessor()));
        assertEquals(List.of(AlphaAccessor.class, EpsilonAccessor.class), accessorTypes(builder.build()));
    }

    @Test
    void GivenBuilder_WhenInsertTargetIsMissing_ThenInsertionThrows() {
        AccessorsChain.Builder builder = AccessorsChain.builder(false);

        IllegalStateException missingBefore = assertThrows(
                IllegalStateException.class,
                () -> builder.insertBefore(new AlphaAccessor(), BetaAccessor.class)
        );
        assertTrue(missingBefore.getMessage().contains(BetaAccessor.class.getSimpleName()));

        IllegalStateException missingAfter = assertThrows(
                IllegalStateException.class,
                () -> builder.insertAfter(new AlphaAccessor(), BetaAccessor.class)
        );
        assertTrue(missingAfter.getMessage().contains(BetaAccessor.class.getSimpleName()));
    }

    @Test
    void GivenBuilderWithMultipleAccessors_WhenInsertAfterNonFirstAccessor_ThenInsertsAtCorrectPosition() {
        AccessorsChain.Builder builder = AccessorsChain.builder(false)
                .addLast(new AlphaAccessor())
                .addLast(new BetaAccessor());

        builder.insertAfter(new GammaAccessor(), BetaAccessor.class);

        assertEquals(List.of(AlphaAccessor.class, BetaAccessor.class, GammaAccessor.class), accessorTypes(builder.build()));
    }

    @Test
    void GivenBuilderWithoutTarget_WhenReplace_ThenThrows() {
        AccessorsChain.Builder builder = AccessorsChain.builder(false);

        final var replacement = new BetaAccessor();
        assertThrows(IllegalArgumentException.class, () -> builder.replace(AlphaAccessor.class, replacement));
    }

    @Test
    void GivenDefaults_WhenUseDefaultConstructorOfBuilder_ThenChainHasDefaults() {
        var builder = new AccessorsChain.Builder();

        assertTrue(builder.lastResortEnabled);
        assertTrue(builder.chain.stream().anyMatch(ArrayAccessor.class::isInstance));
        assertTrue(builder.chain.stream().anyMatch(IterativeAccessor.class::isInstance));
        assertTrue(builder.chain.stream().anyMatch(IterativeMapAccessor.class::isInstance));
    }

    @Test
    void GivenFailingAccessor_WhenHandlerSkips_ThenReturnsEmptyCollection() throws AccessorException {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new FailingAccessor(new IllegalStateException("boom")))
                .build();

        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withExceptionHandler(context -> ExceptionResolution.skip())
                .build();

        assertEquals(Collections.emptyList(), chain.extract(new Object(), null, testContext(), settings));
    }

    @Test
    void GivenFailingAccessor_WhenHandlerSubstitutes_ThenReturnsReplacementContent() throws AccessorException {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new FailingAccessor(new IllegalStateException("boom")))
                .build();

        Content replacement = new Content("fallback", null);
        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withExceptionHandler(context -> ExceptionResolution.substitute(List.of(replacement)))
                .build();

        assertEquals(List.of(replacement), chain.extract(new Object(), null, testContext(), settings));
    }

    @Test
    void GivenFatalAccessorException_WhenExtracting_ThenPropagatesWithoutHandling() {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new FailingAccessor(AccessorException.fatal("fatal", new IllegalStateException("boom"))))
                .build();

        AccessorException exception = assertThrows(
                AccessorException.class,
                () -> chain.extract(new Object(), null, testContext(), IntrospectionSettings.builder().build())
        );

        assertTrue(exception.isFatal());
        assertEquals("fatal", exception.getMessage());
    }

    @Test
    void GivenNonFatalAccessorException_WhenHandlerPropagates_ThenWrapsFailureAsFatal() {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new FailingAccessor(new AccessorException("non-fatal")))
                .build();

        IntrospectionSettings settings = IntrospectionSettings.builder()
                .withExceptionHandler(context -> ExceptionResolution.propagate())
                .build();

        AccessorException exception = assertThrows(
                AccessorException.class,
                () -> chain.extract(new Object(), null, testContext(), settings)
        );

        assertTrue(exception.isFatal());
        assertTrue(exception.getMessage().contains("FailingAccessor failed to extract content from Object"));
    }

    @Test
    void GivenNoAccessorAcceptsType_WhenExtract_ThenReturnsEmptyCollection() throws AccessorException {
        AccessorsChain chain = AccessorsChain.builder(false)
                .addLast(new AlphaAccessor())
                .addLast(new BetaAccessor())
                .build();

        assertEquals(Collections.emptyList(),
                chain.extract(new Object(), null, testContext(), IntrospectionSettings.builder().build()));
    }

    private static IntrospectionContext<Object> testContext() {
        return new IntrospectionContext<>(Object.class, MethodHandles.lookup(), Set.of(), Set.of(), AccessorsChain.builder(false).build());
    }

    private static final class FailingAccessor implements ContentAccessor {
        private final Throwable throwable;

        private FailingAccessor(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public boolean accepts(Class<?> current, java.lang.reflect.Field holdingField) {
            return true;
        }

        @Override
        public Collection<Content> extract(Object current, java.lang.reflect.Field holdingField,
                                           IntrospectionContext<?> context,
                                           IntrospectionSettings settings) throws AccessorException {
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (throwable instanceof AccessorException accessorException) {
                throw accessorException;
            }
            throw new RuntimeException(throwable);
        }
    }

}