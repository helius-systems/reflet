package systems.helius.reflet;

import com.github.javafaker.Faker;
import com.sb.factorium.FactoryProvider;
import com.sb.factorium.RandomUtil;
import com.sb.factorium.RecordingFactory;
import com.sb.factorium.RecordingFactoryMaker;
import org.junit.jupiter.api.Test;
import systems.helius.reflet.accessors.Content;
import systems.helius.reflet.accessors.ContentAccessor;
import systems.helius.reflet.exceptions.ExceptionResolution;
import systems.helius.reflet.exceptions.IntrospectionException;
import systems.helius.reflet.fixtures.*;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class BeanIntrospectorTest {
    private static Faker faker = new Faker();

    private static FooGenerator fooGenerator = new FooGenerator();
    private static FooCollectionGenerator fooCollectionGenerator = new FooCollectionGenerator(fooGenerator);

    @Test
    void GivenObjectWithInheritance_WhenSeekInt_ThenAlsoFindInSuperclass() throws IntrospectionException {
        int first = 1;
        int second = 6;
        DataClassWithoutGetters simple = new DataClassWithoutGetters(
                first,
                second,
                "Hello",
                "World",
                9L,
                -15,
                first
        );
        Set<Integer> found = new BeanIntrospector().seek(int.class, simple, MethodHandles.lookup());
        assertEquals(2, found.size());
        assertTrue(found.contains(first));
        assertTrue(found.contains(second));
    }

    @Test
    void GivenSimpleClass_WhenSeekInt_ThenFindAll() throws IntrospectionException {
        var foo = fooGenerator.generate();
        Set<Integer> found = new BeanIntrospector().seek(int.class, foo, MethodHandles.lookup());
        assertEquals(1, found.size());
        assertTrue(found.stream().anyMatch(f -> f.equals(foo.getA())));
    }

    @Test
    void GivenCollectionsWrapper_WhenSeekInt_ThenFindAll() throws IntrospectionException {
        FooCollection fooCollection = fooCollectionGenerator.generate();
        Set<Foo> found = new BeanIntrospector().seek(Foo.class, fooCollection, MethodHandles.lookup());
        assertEquals(fooCollection.totalElements(), found.size());
    }

    @Test
    void GivenClassWithIterables_WhenSeekIterable_ThenFindIterables() throws IntrospectionException {
        FooCollection fooCollection = fooCollectionGenerator.generate();
        //noinspection rawtypes impossible to cast the generic of Iterable
        Set<Iterable> found = new BeanIntrospector().seek(Iterable.class, fooCollection, MethodHandles.lookup());
        assertEquals(2, found.size());
    }

    @Test
    void GivenObjectArray_WhenSeekObjectArrayContent_ThenFindAll() throws IntrospectionException {
        Foo[] arr = fooGenerator.generate(5).toArray(new Foo[0]);
        Set<Foo> found = new BeanIntrospector().seek(Foo.class, arr, MethodHandles.lookup());
        assertEquals(arr.length, found.size());
    }

    @Test
    void GivenPrimitiveArray_WhenSeekPrimitiveArrayContent_ThenFindAll() throws IntrospectionException {
        int[] arr = ThreadLocalRandom.current().ints(5).toArray();
        Set<Integer> found = new BeanIntrospector().seek(int.class, arr, MethodHandles.lookup());
        assertEquals(arr.length, found.size());
    }

    @Test
    void GivenNestedObjectArray_WhenSeekObjectArrayContent_ThenFindAll() throws IllegalAccessException, IntrospectionException {
        RecordingFactory<String, Foo> recordingFactory = (RecordingFactory<String, Foo>) FactoryProvider.make(
                List.of(fooGenerator), FactoryProvider.DefaultKey.DEFAULT_KEY, new RecordingFactoryMaker(), false)
                .factory(Foo.class);
        Foo[][][][] multiDimensionalArray = new Foo[3][3][3][];
        for (int i = 0; i < multiDimensionalArray.length; i++) {
            for (int j = 0; j < multiDimensionalArray[i].length; j++) {
                for (int k = 0; k < multiDimensionalArray[i][j].length; k++) {
                    multiDimensionalArray[i][j][k] = recordingFactory.generate(3).toArray(new Foo[0]);
                }
            }
        }
        Set<Foo> foos = new BeanIntrospector().seek(Foo.class, multiDimensionalArray, MethodHandles.lookup());
        assertEquals(recordingFactory.getCreated().size(), foos.size());
    }

    @Test
    void GivenNestedPrimitiveArray_WhenSeekPrimitiveArrayContent_ThenFindAll() throws IntrospectionException {
        final int LOWEST_LEVEL_SIZE = 3;
        int nGenerated = 0;
        long[][][][] multiDimensionalArray = new long[3][3][3][];
        for (int i = 0; i < multiDimensionalArray.length; i++) {
            for (int j = 0; j < multiDimensionalArray[i].length; j++) {
                for (int k = 0; k < multiDimensionalArray[i][j].length; k++) {
                    multiDimensionalArray[i][j][k] = ThreadLocalRandom.current().longs(LOWEST_LEVEL_SIZE).toArray();
                    nGenerated += LOWEST_LEVEL_SIZE;
                }
            }
        }
        Set<Long> found = new BeanIntrospector().seek(long.class, multiDimensionalArray, MethodHandles.lookup());
        assertEquals(nGenerated, found.size());
    }

    @Test
    void GivenNestedPrimitiveArray_WhenSeekPrimitiveArray_ThenFindAll() throws IntrospectionException {
        long[][][][] multiDimensionalArray = new long[3][3][3][];
        final int N_GENERATED = multiDimensionalArray.length * multiDimensionalArray[0].length * multiDimensionalArray[0][0].length;
        for (int i = 0; i < multiDimensionalArray.length; i++) {
            for (int j = 0; j < multiDimensionalArray[i].length; j++) {
                for (int k = 0; k < multiDimensionalArray[i][j].length; k++) {
                    multiDimensionalArray[i][j][k] = ThreadLocalRandom.current().longs(1).toArray();
                }
            }
        }
        Set<long[]> found = new BeanIntrospector().seek(long[].class, multiDimensionalArray, MethodHandles.lookup());
        assertEquals(N_GENERATED, found.size());
    }

    @Test
    void GivenClassWithMixedPrimitivesAndWrappers_WhenSeekPrimitiveWrapper_ThenOnlyFindWrappers() throws IntrospectionException {
        int first = -15;
        int second = 16;
        DataClassWithoutGetters simple = new DataClassWithoutGetters(
                1,
                2,
                "Hello",
                "World",
                9L,
                first,
                second
        );
        Set<Integer> found = new BeanIntrospector().seek(Integer.class, simple, MethodHandles.lookup());
        assertEquals(2, found.size());
        assertTrue(found.contains(first));
        assertTrue(found.contains(second));
    }

    @Test
    void GivenReferenceLoop_WhenSeekObject_ThenFindAll() throws IntrospectionException {
        var first = new ChainLink(null);
        var second = new ChainLink(first);
        var third = new ChainLink(second);
        var fourth = new ChainLink(third);
        fourth.setNext(first);

        Set<ChainLink> found = new BeanIntrospector().seek(ChainLink.class, first, MethodHandles.lookup());
        assertEquals(4, found.size());
    }

    @Test
    void GivenComplexStructureWithHiddenStrata_WhenSeek_ThenFindEvenWithinHiddenStrata() throws IntrospectionException {
        var firstId = new ComplexStructure.MiddleStrata.IntHolder(1);
        var secondId = new ComplexStructure.MiddleStrata.IntHolder(2);
        var structure = new ComplexStructure(firstId, secondId);

        Set<ComplexStructure.MiddleStrata.IntHolder> found = new BeanIntrospector().seek(
                ComplexStructure.MiddleStrata.IntHolder.class, structure, MethodHandles.lookup());
        assertEquals(2, found.size());
        assertTrue(found.contains(firstId));
        assertTrue(found.contains(secondId));
    }

    @Test
    void GivenEqualButDifferentInstances_WhenSeek_ThenFindAll() throws IntrospectionException {
        var firstId = new ComplexStructure.MiddleStrata.IntHolder(1);
        var secondId = new ComplexStructure.MiddleStrata.IntHolder(1);
        var structure = new ComplexStructure(firstId, secondId);

        Set<ComplexStructure.MiddleStrata.IntHolder> found = new BeanIntrospector().seek(
                ComplexStructure.MiddleStrata.IntHolder.class, structure, MethodHandles.lookup());
        assertEquals(2, found.size());
        assertTrue(found.contains(firstId));
        assertTrue(found.contains(secondId));
    }

    @Test
    void GivenSharedInstances_WhenSeek_ThenFindOnlyDifferentInstances() throws IntrospectionException {
        var firstId = new ComplexStructure.MiddleStrata.IntHolder(1);
        var structure = new ComplexStructure(firstId, firstId);

        Set<ComplexStructure.MiddleStrata.IntHolder> found = new BeanIntrospector().seek(
                ComplexStructure.MiddleStrata.IntHolder.class, structure, MethodHandles.lookup());
        assertEquals(1, found.size());
        assertTrue(found.contains(firstId));
    }

    @Test
    void GivenEnum_WhenSeekInt_ThenFindIdField() throws IntrospectionException {
        BarEnum bar = RandomUtil.randomEnum(BarEnum.class);
        Set<Integer> found = new BeanIntrospector().seek(int.class, bar, MethodHandles.lookup());
        assertEquals(1, found.size());
        assertEquals(bar.getId(), found.iterator().next());
    }

    @Test
    void GivenMap_WhenSeekMapContent_ThenCanReadKey() throws IntrospectionException {
        List<String> toFind = faker.lorem().words(2);
        var map = new HashMap<>();
        // Put noise in the map
        map.put(Math.random(), Math.random());
        map.put(Math.random(), Math.random());
        map.put(Math.random(), Math.random());
        toFind.forEach(s -> map.put(s, Math.random()));

        Set<String> found = new BeanIntrospector().seek(String.class, map, MethodHandles.lookup());
        assertEquals(toFind.size(), found.size());
        assertTrue(found.containsAll(toFind));
    }

    @Test
    void GivenMap_WhenSeekMapContent_ThenCanReadValues() throws IntrospectionException {
        List<String> toFind = List.of("Hello", "World", "Foo");
        var map = new HashMap<>();
        // Put noise in the map
        map.put(Math.random(), Math.random());
        map.put(Math.random(), Math.random());
        toFind.forEach(s -> map.put(Math.random() * 100, s));
        map.put(Math.random(), Math.random());

        Set<String> found = new BeanIntrospector().seek(String.class, map, MethodHandles.lookup());
        assertEquals(toFind.size(), found.size());
        assertTrue(found.containsAll(toFind));
    }

    @Test
    void GivenUseUnsafeAccessAndOutOfModuleCode_WhenSeekMapContent_ThenFail() {
        var settings = IntrospectionSettings.builder()
                .withAccessDenialPolicy(AccessDenialPolicy.FAIL)
                .build();
        var introspector = new BeanIntrospector(settings);
        var map = new HashMap<String, Boolean>();
        map.put("hello", true);
        map.put("world", false);
        assertThrows(IntrospectionException.class, () -> introspector.seek(String.class, map, MethodHandles.lookup()));
    }

    @Test
    void GivenAccessorFailureAndSkipResolution_WhenSeek_ThenIgnoreFailure() throws IntrospectionException {
        var settings = IntrospectionSettings.builder()
                .withExceptionHandler(context -> ExceptionResolution.skip())
                .withContentAccessor(new ThrowingAccessor(Object.class, new IllegalStateException("boom")))
                .build();

        var found = new BeanIntrospector(settings).seek(String.class, new Object(), MethodHandles.lookup());

        assertTrue(found.isEmpty());
    }

    @Test
    void GivenAccessorFailureAndSubstituteResolution_WhenSeek_ThenUseSubstitute() throws IntrospectionException {
        var replacement = "recovered";
        var settings = IntrospectionSettings.builder()
                .withExceptionHandler(context -> ExceptionResolution.substitute(List.of(new Content(replacement, null))))
                .withContentAccessor(new ThrowingAccessor(Object.class, new IllegalStateException("boom")))
                .build();

        var found = new BeanIntrospector(settings).seek(String.class, new Object(), MethodHandles.lookup());

        assertEquals(Set.of(replacement), found);
    }

    @Test
    void GivenAccessorFailureAndPropagateResolution_WhenSeek_ThenThrowIntrospectionException() {
        var settings = IntrospectionSettings.builder()
                .withExceptionHandler(context -> ExceptionResolution.propagate())
                .withContentAccessor(new ThrowingAccessor(Object.class, new IllegalStateException("boom")))
                .build();

        assertThrows(IntrospectionException.class,
                () -> new BeanIntrospector(settings).seek(String.class, new Object(), MethodHandles.lookup()));
    }

    @Test
    void GivenEnterTargetTypeFalse_WhenReachingTargetType_ThenDoNotEnter() throws IntrospectionException {
        var settings = IntrospectionSettings.builder()
                .withEnterTargetType(false)
                .build();
        var introspector = new BeanIntrospector(settings);
        var selfReferencingObject = new SelfReferencing(new SelfReferencing(null));

        Set<SelfReferencing> found = introspector.seek(SelfReferencing.class, selfReferencingObject, MethodHandles.lookup());

        assertEquals(1, found.size());
    }

    private record ThrowingAccessor(Class<?> failingType, RuntimeException exception) implements ContentAccessor {
        @Override
        public boolean accepts(Class<?> current, Field holdingField) {
            return current == failingType;
        }

        @Override
        public Collection<Content> extract(Object current, Field holdingField, IntrospectionContext<?> context,
                                          IntrospectionSettings settings) {
            if (current.getClass() == failingType) {
                throw exception;
            }
            return Collections.emptyList();
        }
    }
}