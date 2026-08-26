package systems.helius.reflet.reflection.accessors;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused") // We use reflection to access the fields in this class
final class SyntheticPrimitiveFields {
    private static final Map<Class<?>, Field> TYPES_TO_FIELDS;
    static {
        TYPES_TO_FIELDS = new LinkedHashMap<>();
        for (Field field : SyntheticPrimitiveFields.class.getDeclaredFields()) {
            TYPES_TO_FIELDS.put(field.getType(), field);
        }
    }

    boolean mBool;
    boolean[] booleanArray;
    byte mByte;
    byte[] byteArray;
    short mShort;
    short[] shortArray;
    int mInt;
    int[] intArray;
    long mLong;
    long[] longArray;
    float mFloat;
    float[] floatArray;
    double mDouble;
    double[] doubleArray;
    char aChar;
    char[] charArray;

    private SyntheticPrimitiveFields() {}

    public static Field getSyntheticPrimitiveField(Class<?> type) {
        Field result = TYPES_TO_FIELDS.get(type);
        if (result == null) {
            throw new IllegalArgumentException("No synthetic primitive field for type: " + type);
        }
        return result;
    }
}
