/**
 * Reflet provides tools for reflection and metaprogramming.
 */
module systems.helius.reflet {
    requires transitive jakarta.annotation;

    exports systems.helius.reflet;
    exports systems.helius.reflet.exceptions;
    exports systems.helius.reflet.accessors;
}