/**
 * Reflet provides tools for reflection and metaprogramming.
 */
module systems.helius.reflet {
    requires transitive jakarta.annotation;

    exports systems.helius.reflet.exceptions;
    exports systems.helius.reflet.reflection;
    exports systems.helius.reflet.reflection.accessors;
}