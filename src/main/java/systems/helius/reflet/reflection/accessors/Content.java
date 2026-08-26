package systems.helius.reflet.reflection.accessors;


import java.lang.reflect.Field;

/**
 * Represents a part of an object, including its value and the field that holds it.
 *
 * @param value        the value contained by the object
 * @param holdingField the field that holds this value
 */
public record Content(Object value, Field holdingField) {
}
