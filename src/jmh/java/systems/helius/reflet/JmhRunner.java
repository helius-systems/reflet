package systems.helius.reflet;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import systems.helius.reflet.reflection.BeanIntrospectorBenchmark;
import systems.helius.reflet.reflection.ClassInspectorBenchmark;
import systems.helius.reflet.reflection.LookupManagerBenchmark;

public class JmhRunner {

    // Heavily inspired from https://mkyong.com/java/java-jmh-benchmark-tutorial/
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BeanIntrospectorBenchmark.class.getName())
                .include(ClassInspectorBenchmark.class.getName())
                .include(LookupManagerBenchmark.class.getName())
                .build();

        new Runner(opt).run();
    }
}
