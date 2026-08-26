package systems.helius.reflet.fixtures;

import java.util.Objects;

public class Foo {
    private int a;
    private String b;

    public Foo(int a, String b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Foo foo)) return false;
        return a == foo.a && Objects.equals(b, foo.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public String toString() {
        return "Foo{" +
                "a=" + a +
                ", b='" + b + '\'' +
                '}';
    }
}
