package systems.helius.reflet.reflection;

import java.util.Map;

public final class TypeUtils {
    private TypeUtils() {}

    /**
     * Wrapper types of Java lang primitives.
     * Key: Wrapper class
     * Value: primitive class
     */
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVES = Map.of(
            Boolean.class, boolean.class,
            Byte.class, byte.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class,
            Character.class, char.class
    );

    /**
     * Checks if the given class is one for a wrapper of a primitive type.
     * @param clazz the type
     * @return true if it is one of the followings:
     * <ul>
     *     <li>Boolean</li>
     *     <li>Byte</li>
     *     <li>Short</li>
     *     <li>Integer</li>
     *     <li>Long</li>
     *     <li>Float</li>
     *     <li>Double</li>
     *     <li>Character</li>
     * </ul>
     */
    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        return WRAPPER_TO_PRIMITIVES.containsKey(clazz);
    }
}
