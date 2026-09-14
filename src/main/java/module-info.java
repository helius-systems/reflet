/**
 * Reflet provides tools for reflection and metaprogramming.
 */
module systems.helius.reflet {
    requires org.jspecify;
    requires org.jetbrains.annotations;

    exports systems.helius.reflet;
    exports systems.helius.reflet.exceptions;
    exports systems.helius.reflet.accessors;
    exports systems.helius.reflet.internal; // Because Result is internal, but is exposed through public methods.
}