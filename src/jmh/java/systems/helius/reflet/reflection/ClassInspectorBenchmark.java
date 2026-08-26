package systems.helius.reflet.reflection;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 2)
@Warmup(time = 5, iterations = 5)
@Measurement(time = 5, iterations = 5)
public class ClassInspectorBenchmark {

    @Benchmark
    public void getAllFieldsHierarchical_noCache(Basic inspector, ExecutionPlan plan, Blackhole bh) {
        bh.consume(inspector.inspector.getAllFieldsHierarchical(plan.classToInspect));
    }

    @Benchmark
    public void getAllFieldsHierarchical_cached(Caching inspector, ExecutionPlan plan, Blackhole bh) {
        bh.consume(inspector.inspector.getAllFieldsHierarchical(plan.classToInspect));
    }

    @State(Scope.Benchmark)
    public static class ExecutionPlan {
        @Param(value = {
                "systems.helius.reflet.fixtures.Foo",
                "systems.helius.reflet.fixtures.DataClass",
                "systems.helius.reflet.fixtures.ChildClassA",
                "systems.helius.reflet.fixtures.ChildClassB",
                "systems.helius.reflet.fixtures.ComplexChild"
        })
        String className;

        Class<?> classToInspect;

        @Setup
        public void getClassToInspect() {
            try {
                System.out.println("Setting class to inspect: " + className);
                classToInspect = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class Caching {
        CachingClassInspector inspector;

        @Setup(Level.Iteration)
        public void initialize() {
            System.out.println("Initializing CachingClassInspector");
            inspector = new CachingClassInspector();
        }
    }

    @State(Scope.Benchmark)
    public static class Basic {
        ClassInspector inspector;

        @Setup(Level.Trial)
        public void initializeNotCaching() {
            System.out.println("Initializing ClassInspector");
            inspector = new ClassInspector();
        }
    }
}
