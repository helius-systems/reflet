package systems.helius.reflet.fixtures;

/**
 * A record with at least one field.
 * @param a
 * @param b
 */
public record RecordType(int a, int b) {
    public boolean isContained(int c) {
        return a == c || b == c;
    }
}
