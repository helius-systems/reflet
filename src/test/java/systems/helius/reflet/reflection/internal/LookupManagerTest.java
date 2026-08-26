package systems.helius.reflet.reflection.internal;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LookupManager}, covering successful acquisition, caching, denial of inaccessible
 * targets, fallback selection, and the security gate that prevents leaking cached lookups to callers
 * that are not themselves entitled.
 */
class LookupManagerTest {

    /** Full-power lookup of this (test) class; same module and nest as {@link Secret}. */
    private static final Lookup LOCAL = MethodHandles.lookup();

    /**
     * Verifies that an entitled caller receives a privileged lookup capable of reading private fields.
     *
     * @throws Throwable if the var handle cannot be resolved or read.
     */
    @Test
    void GivenAccessibleClass_WhenGetPrivilegedLookup_ThenReturnsWorkingLookup() throws Throwable {
        LookupManager manager = new LookupManager();

        Lookup lookup = manager.getPrivilegedLookup(Secret.class, LOCAL);
        VarHandle handle = lookup.findVarHandle(Secret.class, "value", String.class);

        assertEquals("hidden", handle.get(new Secret()));
    }

    /**
     * Verifies that repeated requests for the same target reuse the cached privileged lookup.
     *
     * @throws LoookupAcquisitionException never in this scenario.
     */
    @Test
    void GivenRepeatedRequests_WhenGetPrivilegedLookup_ThenReusesCachedLookup() throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();

        Lookup first = manager.getPrivilegedLookup(Secret.class, LOCAL);
        Lookup second = manager.getPrivilegedLookup(Secret.class, LOCAL);

        assertSame(first, second, "the second request should reuse the cached lookup");
    }

    /**
     * Verifies that a JDK class, whose package is never opened to the class path, is denied without
     * leaking the costly {@link IllegalAccessException} as the public failure mode.
     */
    @Test
    void GivenJdkClass_WhenGetPrivilegedLookup_ThenThrowsAcquisitionException() {
        LookupManager manager = new LookupManager();

        assertThrows(LoookupAcquisitionException.class,
                () -> manager.getPrivilegedLookup(Integer.class, LOCAL));
    }

    /**
     * Verifies that an entitled fallback is used when the primary caller cannot grant access.
     *
     * @throws LoookupAcquisitionException never in this scenario.
     */
    @Test
    void GivenDenyingCallerButEntitledFallback_WhenGetPrivilegedLookup_ThenUsesFallback()
            throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();

        Lookup lookup = manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup(), LOCAL);

        assertNotNull(lookup);
        assertEquals(Secret.class, lookup.lookupClass());
    }

    /**
     * Verifies the security gate: once a lookup is cached by an entitled caller, an unentitled caller
     * must still be denied rather than handed the cached privileged lookup.
     *
     * @throws LoookupAcquisitionException never while priming the cache.
     */
    @Test
    void GivenCachedLookup_WhenUnentitledCallerRequests_ThenStillDenied() throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();
        manager.getPrivilegedLookup(Secret.class, LOCAL); // prime the cache with an entitled caller

        assertThrows(LoookupAcquisitionException.class,
                () -> manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup()));
    }

    /**
     * Verifies that {@link LookupManager#canAccess(Class, Lookup)} mirrors the preconditions of
     * {@link MethodHandles#privateLookupIn(Class, Lookup)} for the representative cases.
     */
    @Test
    void GivenVariousTargets_WhenCanAccess_ThenMirrorsPrivateLookupInRules() {
        assertTrue(LookupManager.canAccess(Secret.class, LOCAL), "same-module nestmate is accessible");
        assertFalse(LookupManager.canAccess(Integer.class, LOCAL), "java.base does not open to the class path");
        assertFalse(LookupManager.canAccess(int.class, LOCAL), "primitives have no private lookup");
        assertFalse(LookupManager.canAccess(String[].class, LOCAL), "array types have no private lookup");
        assertFalse(LookupManager.canAccess(Secret.class, MethodHandles.publicLookup()),
                "a public lookup lacks PRIVATE/MODULE modes");
    }

    /**
     * Sample type with a private field used to validate that the acquired lookup grants private access.
     */
    private static class Secret {
        @SuppressWarnings("unused")
        private final String value = "hidden";
    }
}

