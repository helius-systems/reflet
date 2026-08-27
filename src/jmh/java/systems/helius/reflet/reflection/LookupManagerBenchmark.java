package systems.helius.reflet.reflection;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import systems.helius.reflet.exceptions.LoookupAcquisitionException;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks {@link LookupManager#getPrivilegedLookup(Class, java.lang.invoke.MethodHandles.Lookup,
 * java.lang.invoke.MethodHandles.Lookup...)} on the two dominant introspection paths:
 *
 * <ul>
 *     <li><b>Cache-hit success</b> ({@link #resolveAccessibleCached}): an entitled caller resolves a
 *     class that is already cached. This is the steady-state cost paid for every reachable
 *     application object during a traversal.</li>
 *     <li><b>Denial</b> ({@link #resolveDenied}): a class on the module-protected JDK boundary is
 *     resolved. Denial is the common case (every JDK class refuses private access to class-path
 *     code); the optimized manager rejects it with a cheap module/package predicate instead of the
 *     costly {@link IllegalAccessException} that {@code privateLookupIn} throws.</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 2)
@Warmup(time = 5, iterations = 5)
@Measurement(time = 10, iterations = 5)
public class LookupManagerBenchmark {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * Measures resolving an accessible, already-cached class (steady-state introspection cost).
     *
     * @param state per-trial benchmark state.
     * @param bh    JMH sink to prevent dead-code elimination.
     * @throws LoookupAcquisitionException never for the accessible target.
     */
    @Benchmark
    public void resolveAccessibleCached(ManagerState state, Blackhole bh) throws LoookupAcquisitionException {
        bh.consume(state.manager.getPrivilegedLookup(Accessible.class, LOOKUP));
    }

    /**
     * Measures resolving a module-protected JDK class, exercising the denial path.
     *
     * @param state per-trial benchmark state.
     * @param bh    JMH sink to prevent dead-code elimination.
     */
    @Benchmark
    public void resolveDenied(ManagerState state, Blackhole bh) {
        try {
            bh.consume(state.manager.getPrivilegedLookup(Integer.class, LOOKUP));
        } catch (LoookupAcquisitionException denied) {
            bh.consume(denied);
        }
    }

    /**
     * Holds a {@link LookupManager} whose cache is primed for the accessible target so that
     * {@link #resolveAccessibleCached} measures the cache-hit path.
     */
    @State(Scope.Benchmark)
    public static class ManagerState {
        LookupManager manager;

        /**
         * Initializes and primes the manager once per trial.
         *
         * @throws LoookupAcquisitionException never for the accessible target.
         */
        @Setup(Level.Trial)
        public void initialize() throws LoookupAcquisitionException {
            manager = new LookupManager();
            manager.getPrivilegedLookup(Accessible.class, LOOKUP); // prime the cache
        }
    }

    /**
     * Sample accessible type with a private field, in the same module as the benchmark.
     */
    private static final class Accessible {
        @SuppressWarnings("unused")
        private final int value = 42;
    }
}

