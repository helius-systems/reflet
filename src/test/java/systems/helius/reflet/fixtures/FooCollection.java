package systems.helius.reflet.fixtures;

import java.util.List;
import java.util.Set;

public class FooCollection {
    private List<Foo> foos;
    private Set<Foo> fooSet;

    public FooCollection() {}

    public FooCollection(List<Foo> foos, Set<Foo> fooSet) {
        this.foos = foos;
        this.fooSet = fooSet;
    }

    public int totalElements() {
        return foos.size() + fooSet.size();
    }
}
