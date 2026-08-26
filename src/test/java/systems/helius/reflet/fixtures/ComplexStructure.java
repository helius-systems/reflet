package systems.helius.reflet.fixtures;

import lombok.EqualsAndHashCode;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@EqualsAndHashCode
public class ComplexStructure {
    public static class MiddleStrata {
        public static class IntHolder {
            int id;

            public IntHolder(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                IntHolder intHolder = (IntHolder) o;
                return id == intHolder.id;
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(id);
            }
        }

        private static class PrivateInnerClass {
            private IntHolder hiddenIntHolder;

            public PrivateInnerClass(IntHolder hiddenIntHolder) {
                this.hiddenIntHolder = hiddenIntHolder;
            }
        }

        private PrivateInnerClass privateInnerClass;

        public MiddleStrata(IntHolder hiddenIntHolder) {
            this.privateInnerClass = new PrivateInnerClass(hiddenIntHolder);
        }

        @Override
        public String toString() {
            return "MiddleStrata{" +
                    "id=" + privateInnerClass.hiddenIntHolder.id +
                    '}';
        }
    }

    private MiddleStrata a;
    private MiddleStrata b;

    protected ComplexStructure() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        this.a = new MiddleStrata(new MiddleStrata.IntHolder(rng.nextInt()));
        this.b = new MiddleStrata(new MiddleStrata.IntHolder(rng.nextInt()));
    }

    public ComplexStructure(MiddleStrata.IntHolder idOfA, MiddleStrata.IntHolder idOfB) {
        this.a = new MiddleStrata(idOfA);
        this.b = new MiddleStrata(idOfB);
    }
}
