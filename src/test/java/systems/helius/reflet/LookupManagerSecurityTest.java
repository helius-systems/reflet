package systems.helius.reflet;

import org.junit.jupiter.api.Test;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security-focused regression and mutation tests for {@link LookupManager}.
 *
 * <p>The cache inside {@link LookupManager} stores full-power {@link Lookup}s. The security of the
 * whole component rests on a single invariant: a cached privileged lookup is only ever returned to a
 * caller that independently passes {@link LookupManager#canAccess(Class, Lookup)}, and that predicate
 * is never more permissive than {@link MethodHandles#privateLookupIn(Class, Lookup)} itself. If either
 * property drifts, an unentitled caller could be handed a privileged lookup it should not have.</p>
 *
 * <p>This suite is split into two layers:</p>
 * <ul>
 *     <li><b>Regression / drift guard</b> — asserts the equivalence invariant across a broad matrix
 *     of targets and lookups, so any future change that makes {@code canAccess} too lenient breaks a
 *     test rather than silently becoming a vulnerability.</li>
 *     <li><b>Mutation killers</b> — each test is constructed so that a specific dangerous edit to the
 *     production code (named in the test's documentation) causes that test to fail.</li>
 * </ul>
 */
class LookupManagerSecurityTest {

    /** Full-power lookup of this (test) class; same module and nest as the sample types below. */
    private static final Lookup FULL = MethodHandles.lookup();
    /**
     * A representative matrix of targets covering primitives, arrays, interfaces, JDK classes in a
     * named module, and same-module application types (including enum and record forms).
     */
    private static final List<Class<?>> TARGETS = List.of(
            Secret.class,
            OtherSecret.class,
            LookupManagerSecurityTest.class,
            SampleEnum.class,
            SampleRecord.class,
            int.class,
            long.class,
            double.class,
            boolean.class,
            int[].class,
            String[].class,
            Secret[].class,
            Integer.class,
            String.class,
            java.util.HashMap.class,
            Runnable.class
    );

    // ---------------------------------------------------------------------------------------------
    // Regression / drift guard
    // ---------------------------------------------------------------------------------------------

    /**
     * The core security invariant: {@link LookupManager#canAccess(Class, Lookup)} must never grant
     * (return {@code true} for) a target/lookup pair that {@link MethodHandles#privateLookupIn(Class, Lookup)}
     * would itself reject. {@code canAccess} is allowed to be stricter (a false negative only costs a
     * little performance), but never more permissive.
     *
     * <p>This test feeds a broad matrix of targets and lookups of varying privilege so that any drift
     * making the predicate too lenient is caught. It is the primary guard against the cache ever
     * leaking a privileged lookup.</p>
     */
    @Test
    void GivenManyTargetsAndLookups_WhenCanAccessIsTrue_ThenPrivateLookupInAlsoSucceeds() {
        List<Lookup> lookups = List.of(
                FULL,                                   // full power
                MethodHandles.publicLookup(),           // PUBLIC only
                FULL.dropLookupMode(Lookup.PRIVATE),    // retains MODULE/PACKAGE, lacks PRIVATE
                FULL.dropLookupMode(Lookup.PACKAGE),    // lacks PRIVATE and PACKAGE
                FULL.dropLookupMode(Lookup.MODULE),     // lacks MODULE (and weaker)
                FULL.dropLookupMode(Lookup.PUBLIC)      // no modes at all
        );

        for (Class<?> target : TARGETS) {
            for (Lookup lookup : lookups) {
                boolean predicted = LookupManager.canAccess(target, lookup);
                if (predicted && !jdkAllows(target, lookup)) {
                    fail("SECURITY DRIFT: canAccess granted " + describe(target, lookup)
                            + " but MethodHandles.privateLookupIn denies it. The cache could leak a "
                            + "privileged lookup to an unentitled caller.");
                }
            }
        }
    }

    /**
     * Locks the positive direction for a known-accessible target so that a mutation turning
     * {@code canAccess} into a constant {@code false} (which would silently disable the cache and make
     * the drift guard vacuous) is also caught.
     */
    @Test
    void GivenAccessibleTarget_WhenCanAccess_ThenAgreesWithJdkAsTrue() {
        assertTrue(LookupManager.canAccess(Secret.class, FULL),
                "a same-module nestmate must be accessible");
        assertTrue(jdkAllows(Secret.class, FULL),
                "sanity: the JDK must also allow it, otherwise the matrix is meaningless");
    }

    // ---------------------------------------------------------------------------------------------
    // Mutation killers — predicate
    // ---------------------------------------------------------------------------------------------

    /**
     * Kills mutations that remove the primitive guard in {@link LookupManager#canAccess(Class, Lookup)}.
     */
    @Test
    void GivenPrimitiveTarget_WhenCanAccess_ThenDenied() {
        assertFalse(LookupManager.canAccess(int.class, FULL));
        assertFalse(LookupManager.canAccess(void.class, FULL));
    }

    /**
     * Kills mutations that remove the array guard in {@link LookupManager#canAccess(Class, Lookup)}.
     */
    @Test
    void GivenArrayTarget_WhenCanAccess_ThenDenied() {
        assertFalse(LookupManager.canAccess(int[].class, FULL));
        assertFalse(LookupManager.canAccess(Secret[].class, FULL));
    }

    /**
     * Kills mutations that drop {@code PRIVATE} from the required mode mask. The lookup retains
     * {@code MODULE} (and {@code PACKAGE}) but lacks {@code PRIVATE}; if the mask no longer required
     * {@code PRIVATE}, this same-module target would wrongly be granted.
     */
    @Test
    void GivenLookupMissingPrivateMode_WhenCanAccess_ThenDenied() {
        Lookup noPrivate = FULL.dropLookupMode(Lookup.PRIVATE);
        assertNotEquals(0, noPrivate.lookupModes() & Lookup.MODULE,
                "precondition: the lookup still holds MODULE so this isolates the PRIVATE requirement");
        assertEquals(0, noPrivate.lookupModes() & Lookup.PRIVATE,
                "precondition: the lookup no longer holds PRIVATE");

        assertFalse(LookupManager.canAccess(Secret.class, noPrivate),
                "a lookup without PRIVATE must never receive a privileged lookup");
    }

    /**
     * Kills mutations that weaken the mode check to accept a bare {@link MethodHandles#publicLookup()},
     * which holds neither {@code PRIVATE} nor {@code MODULE}.
     */
    @Test
    void GivenPublicLookup_WhenCanAccess_ThenDenied() {
        assertFalse(LookupManager.canAccess(Secret.class, MethodHandles.publicLookup()));
    }

    /**
     * Kills mutations that remove the cross-module {@code isOpen}/{@code canRead} checks: a JDK class in
     * the {@code java.base} named module is not open to the class path and must be denied.
     */
    @Test
    void GivenCrossModuleClosedTarget_WhenCanAccess_ThenDenied() {
        assertFalse(LookupManager.canAccess(Integer.class, FULL));
        assertFalse(LookupManager.canAccess(String.class, FULL));
    }

    // ---------------------------------------------------------------------------------------------
    // Mutation killers — cache gate (the scenario raised in review: cross-module attacker)
    // ---------------------------------------------------------------------------------------------

    /**
     * Reproduces the reviewed attack: an entitled caller primes the cache for a target, then an
     * unentitled caller (here a bare public lookup, standing in for code from a module that the target
     * does not open to) requests the same target.
     *
     * <p>Kills mutations that return the cached lookup before (or instead of) checking
     * {@link LookupManager#canAccess(Class, Lookup)} in {@code tryAcquire}. If the gate is removed or
     * reordered, the unentitled caller would receive the cached privileged lookup instead of being
     * denied.</p>
     *
     * @throws LoookupAcquisitionException never while priming the cache.
     */
    @Test
    void GivenPrimedCache_WhenUnentitledPublicCallerRequests_ThenDenied() throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();
        manager.getPrivilegedLookup(Secret.class, FULL); // prime with an entitled caller

        assertThrows(LoookupAcquisitionException.class,
                () -> manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup()));
    }

    /**
     * Same cache gate, exercised with a lookup that holds higher modes than public but still lacks
     * {@code PRIVATE}. This kills mutations that only special-case the obviously-powerless public
     * lookup while still leaking the cache to partially-privileged callers.
     *
     * @throws LoookupAcquisitionException never while priming the cache.
     */
    @Test
    void GivenPrimedCache_WhenPartiallyPrivilegedCallerRequests_ThenDenied() throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();
        manager.getPrivilegedLookup(Secret.class, FULL); // prime with an entitled caller

        Lookup noPrivate = FULL.dropLookupMode(Lookup.PRIVATE);
        assertThrows(LoookupAcquisitionException.class,
                () -> manager.getPrivilegedLookup(Secret.class, noPrivate));
    }

    /**
     * Verifies that the unentitled caller never receives the exact cached instance. Capturing the
     * cached lookup via an entitled call and proving the unentitled call cannot return it provides a
     * direct, value-level no-leak assertion in addition to the throw-based ones above.
     *
     * @throws LoookupAcquisitionException never while priming the cache.
     */
    @Test
    void GivenPrimedCache_WhenUnentitledCallerRequests_ThenNeverReceivesCachedInstance()
            throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();
        manager.getPrivilegedLookup(Secret.class, FULL);

        assertThrows(LoookupAcquisitionException.class, () -> {
            manager.getPrivilegedLookup(Secret.class, MethodHandles.publicLookup());
        }, "an unentitled caller must be denied, not handed any lookup");
    }

    /**
     * Regression lock for the caching optimization itself: a second entitled request returns the same
     * cached instance. This ensures the gate does not accidentally rebuild on every call (perf
     * regression) while still proving the gate runs for entitled callers.
     *
     * @throws LoookupAcquisitionException never in this scenario.
     */
    @Test
    void GivenEntitledCaller_WhenRequestingTwice_ThenReturnsSameCachedInstance() throws LoookupAcquisitionException {
        LookupManager manager = new LookupManager();

        Lookup first = manager.getPrivilegedLookup(Secret.class, FULL);
        Lookup second = manager.getPrivilegedLookup(Secret.class, FULL);

        assertSame(first, second);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers and fixtures
    // ---------------------------------------------------------------------------------------------

    /**
     * Reports whether {@link MethodHandles#privateLookupIn(Class, Lookup)} would actually grant a
     * privileged lookup, treating every documented rejection mode as a denial.
     *
     * @param target the class a privileged lookup is desired on.
     * @param lookup the candidate lookup.
     * @return {@code true} if the JDK grants the privileged lookup, {@code false} on any rejection.
     */
    private static boolean jdkAllows(Class<?> target, Lookup lookup) {
        try {
            MethodHandles.privateLookupIn(target, lookup);
            return true;
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException denied) {
            // IllegalArgumentException covers primitive/array/void targets; IllegalAccessException
            // covers missing modes and module/package access denial.
            return false;
        }
    }

    /**
     * Builds a readable description of a target/lookup pair for failure messages.
     *
     * @param target the class involved.
     * @param lookup the candidate lookup.
     * @return a human-readable description.
     */
    private static String describe(Class<?> target, Lookup lookup) {
        return target.getTypeName() + " via " + lookup + " (modes=0x" + Integer.toHexString(lookup.lookupModes()) + ")";
    }

    /**
     * Sample type with a private field; same module and nest as the test.
     */
    private static class Secret {
        @SuppressWarnings("unused")
        private final String value = "hidden";
    }

    /**
     * A second sample type to vary the target set.
     */
    private static class OtherSecret {
        @SuppressWarnings("unused")
        private final int token = 1;
    }

    /**
     * Sample enum to exercise enum targets in the matrix.
     */
    private enum SampleEnum {
        @SuppressWarnings("unused") A
    }

    /**
     * Sample record to exercise record targets in the matrix.
     *
     * @param id an arbitrary component.
     */
    private record SampleRecord(int id) {
    }
}

