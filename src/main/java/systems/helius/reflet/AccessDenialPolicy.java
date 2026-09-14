package systems.helius.reflet;

/**
 * Determines how introspection reacts when a class or member cannot be made accessible.
 */
public enum AccessDenialPolicy {
    /**
     * Skips inaccessible classes or members and continues the search.
     */
    SKIP,

    /**
     * Fails the search when a class or member cannot be made accessible.
     */
    FAIL
}
