package systems.helius.reflet.fixtures;

import com.sb.factorium.BaseGenerator;
import com.sb.factorium.Generator;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class FooCollectionGenerator extends BaseGenerator<FooCollection> {
    private Generator<Foo> fooGenerator;

    public FooCollectionGenerator() {
        this.fooGenerator = new FooGenerator();
    }

    public FooCollectionGenerator(Generator<Foo> fooGenerator) {
        this.fooGenerator = fooGenerator;
    }

    @Override
    protected FooCollection make() {
        Random rng = ThreadLocalRandom.current();
        List<Foo> list = fooGenerator.generate(rng.nextInt(0, 15));
        Set<Foo> set = new HashSet<>(fooGenerator.generate(rng.nextInt(0, 15)));
        return new FooCollection(list, set);
    }
}
