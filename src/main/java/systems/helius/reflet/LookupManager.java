package systems.helius.reflet;

import jakarta.annotation.Nullable;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves privileged (private-level) {@link Lookup}s on target classes so that reflective
 * accessors can read otherwise-inaccessible fields.
 *
 * <p>This manager sits on the hottest path of an introspection: {@code FieldHandlesAccessor}
 * asks for a privileged lookup for (almost) every object it visits. Two optimizations keep that
 * path cheap:</p>
 * <ul>
 *     <li><b>Exception avoidance.</b> {@link MethodHandles#privateLookupIn(Class, Lookup)} reports
 *     denial by throwing {@link IllegalAccessException}, and capturing that exception's stack trace
 *     dominates the cost of a failed acquisition. Because denial is the <em>common</em> case (every
 *     JDK class refuses private access to code on the class path), this manager first evaluates the
 *     cheap {@link #canAccess(Class, Lookup)} predicate, which mirrors the JDK's own preconditions,
 *     and only calls {@code privateLookupIn} when it is expected to succeed.</li>
 *     <li><b>Result caching.</b> A successfully acquired lookup grants full power over its target
 *     class and is therefore reusable for any later access to that same class. Successful lookups are
 *     memoized per target class so repeated traversals of the same types reuse the cached handle.
 *     Cache hits remain gated by {@link #canAccess(Class, Lookup)} for the requesting caller, so a
 *     caller that is not itself entitled is never handed a cached privileged lookup.</li>
 * </ul>
 *
 * <p>Only successful acquisitions are cached. Denied targets (such as JDK classes) are rejected by
 * the cheap predicate on every call and therefore never pin a class loader through the cache.</p>
 *
 * <p>Instances are thread-safe.</p>
 */
public final class LookupManager {

    /**
     * Access modes that {@link MethodHandles#privateLookupIn(Class, Lookup)} requires of its caller
     * before it will grant a full-power lookup.
     */
    private static final int FULL_PRIVILEGE = Lookup.PRIVATE | Lookup.MODULE;

    /**
     * Memoized full-power lookups, keyed by target class. Only successful acquisitions are stored,
     * which bounds the map to classes the caller is genuinely allowed to introspect.
     */
    private final Map<Class<?>, Lookup> privilegedLookups = new ConcurrentHashMap<>();

    /**
     * Attempts to get a privileged (private-level access) lookup on a target class.
     *
     * <p>The {@code caller} is tried first, then each {@code fallback} in order. A candidate is only
     * passed to {@link MethodHandles#privateLookupIn(Class, Lookup)} when {@link #canAccess(Class, Lookup)}
     * predicts success, which avoids the costly {@link IllegalAccessException} thrown on denial.</p>
     *
     * @param target    the class on which a privileged lookup is desired.
     * @param caller    the lookup of the caller or, ideally, of the target class itself.
     * @param fallbacks (optional) fallback lookups that may be tried, such as the original context of
     *                  the request. Used as a backup when the caller may not grant privileged access.
     * @return a privileged lookup on {@code target}.
     * @throws LoookupAcquisitionException if neither the caller nor any fallback can grant private
     *                                     access to {@code target}.
     */
    public Lookup getPrivilegedLookup(Class<?> target, Lookup caller, Lookup... fallbacks)
            throws LoookupAcquisitionException {
        // Read the cache once: a memoized lookup is full-power and valid for every entitled caller.
        Lookup cached = privilegedLookups.get(target);

        Lookup result = tryAcquire(target, caller, cached);
        if (result != null) {
            return result;
        }
        if (fallbacks != null) {
            for (Lookup fallback : fallbacks) {
                result = tryAcquire(target, fallback, cached);
                if (result != null) {
                    return result;
                }
            }
        }

        throw new LoookupAcquisitionException(buildDenialMessage(target, caller, fallbacks));
    }

    /**
     * Resolves a privileged lookup for a single candidate, honoring the cache.
     *
     * @param target    the class a privileged lookup is desired on.
     * @param candidate the candidate lookup to use; may be {@code null}.
     * @param cached    the currently cached lookup for {@code target}, or {@code null} if none.
     * @return a privileged lookup when {@code candidate} is entitled, otherwise {@code null}.
     */
    @Nullable
    private Lookup tryAcquire(Class<?> target, @Nullable Lookup candidate, @Nullable Lookup cached) {
        if (candidate == null || !canAccess(target, candidate)) {
            return null;
        }
        // The candidate is entitled, so a cached full-power lookup may be safely reused.
        if (cached != null) {
            return cached;
        }
        try {
            Lookup acquired = MethodHandles.privateLookupIn(target, candidate);
            // Keep the first cached value if another thread won the race.
            Lookup previous = privilegedLookups.putIfAbsent(target, acquired);
            return previous != null ? previous : acquired;
        } catch (IllegalAccessException e) {
            // canAccess mispredicted (e.g. a SecurityManager vetoed the access). Treat as a denial
            // so the next candidate gets a chance; the cheap predicate already filtered the common cases.
            return null;
        }
    }

    /**
     * Cheaply predicts whether {@link MethodHandles#privateLookupIn(Class, Lookup)} would grant a
     * full-power lookup on {@code target} to {@code lookup}, without incurring the cost of the
     * {@link IllegalAccessException} that the JDK throws on denial.
     *
     * <p>The checks mirror the preconditions enforced by {@code privateLookupIn}: the lookup must
     * hold both {@link Lookup#PRIVATE} and {@link Lookup#MODULE} modes, the target must not be a
     * primitive or array type, the lookup's module must read the target's module, and, when the
     * target lives in a named module, that module must open the target's package to the lookup's
     * module. The predicate is conservative-by-construction: any positive result that the JDK would
     * nonetheless reject is handled gracefully by {@link #tryAcquire(Class, Lookup, Lookup)}.</p>
     *
     * @param target the class a privileged lookup is desired on.
     * @param lookup the candidate lookup.
     * @return {@code true} if a privileged lookup is expected to be granted.
     */
    protected static boolean canAccess(Class<?> target, Lookup lookup) {
        if (target.isPrimitive() || target.isArray()) {
            return false;
        }
        if ((lookup.lookupModes() & FULL_PRIVILEGE) != FULL_PRIVILEGE) {
            return false;
        }
        Module callerModule = lookup.lookupClass().getModule();
        Module targetModule = target.getModule();
        if (callerModule == targetModule) {
            return true;
        }
        if (!callerModule.canRead(targetModule)) {
            return false;
        }
        if (targetModule.isNamed()) {
            return targetModule.isOpen(target.getPackageName(), callerModule);
        }
        return true;
    }

    /**
     * Builds a human-readable explanation of why every candidate failed to grant private access.
     *
     * @param target    the class a privileged lookup was desired on.
     * @param caller    the primary candidate lookup; may be {@code null}.
     * @param fallbacks the fallback candidate lookups; may be {@code null}.
     * @return a message describing each denial.
     */
    private static String buildDenialMessage(Class<?> target, @Nullable Lookup caller, @Nullable Lookup[] fallbacks) {
        StringJoiner reasons = new StringJoiner("; ");
        if (caller != null) {
            reasons.add(describeDenial(target, caller));
        }
        if (fallbacks != null) {
            for (Lookup fallback : fallbacks) {
                if (fallback != null) {
                    reasons.add(describeDenial(target, fallback));
                }
            }
        }
        String detail = reasons.toString();
        if (detail.isEmpty()) {
            detail = "no usable caller lookup was provided";
        }
        return "All access has been denied for " + target.getTypeName() + ": " + detail;
    }

    /**
     * Describes the specific reason a candidate lookup cannot privately access the target, mirroring
     * the order of checks in {@link #canAccess(Class, Lookup)}.
     *
     * @param target the class a privileged lookup was desired on.
     * @param lookup the candidate lookup that was denied.
     * @return a short human-readable reason.
     */
    private static String describeDenial(Class<?> target, Lookup lookup) {
        if (target.isPrimitive()) {
            return lookup + " cannot privately access primitive type " + target.getTypeName();
        }
        if (target.isArray()) {
            return lookup + " cannot privately access array type " + target.getTypeName();
        }
        if ((lookup.lookupModes() & FULL_PRIVILEGE) != FULL_PRIVILEGE) {
            return lookup + " lacks the PRIVATE and MODULE lookup modes";
        }
        Module callerModule = lookup.lookupClass().getModule();
        Module targetModule = target.getModule();
        if (callerModule != targetModule) {
            if (!callerModule.canRead(targetModule)) {
                return callerModule + " does not read " + targetModule;
            }
            if (targetModule.isNamed() && !targetModule.isOpen(target.getPackageName(), callerModule)) {
                return targetModule + " does not open " + target.getPackageName() + " to " + callerModule;
            }
        }
        return "privateLookupIn denied access for " + lookup;
    }
}
