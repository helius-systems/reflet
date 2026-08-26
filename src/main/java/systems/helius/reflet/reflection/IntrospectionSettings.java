package systems.helius.reflet.reflection;

import jakarta.annotation.Nullable;
import systems.helius.reflet.reflection.accessors.AccessorsChain;
import systems.helius.reflet.reflection.accessors.ContentAccessor;

import java.util.function.Predicate;

public class IntrospectionSettings {
    /**
     * If true (default), only fields and methods that may be accessed according to the rules will be made accessible.
     * If false, introspectors will throw an IllegalAccessException if faced with something it is not allowed to access.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/IllegalAccessException.html">Java 17 API: IllegalAccessException</a>
     */
    protected final boolean safeAccessCheck;

    /**
     * If set, fields that cause any exception matched by the predicate when accessed will be skipped.
     * If null or the exception is not matched, introspectors will throw an {@link systems.helius.reflet.exceptions.IntrospectionException}
     * if faced with an exception when accessing a field.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/IllegalAccessException.html">Java 17 API: IllegalAccessException</a>
     */
    protected final Predicate<Exception> skipOnException;

    /**
     * If true (default), instances of the target type will also be introspected for more instances.
     */
    protected final boolean enterTargetType;

    /**
     * How deep in the root object to search for matches.
     */
    protected final int maxDepth;

    protected final ContentAccessor contentAccessor;

    /**
     * Default introspection settings.
     */
    public IntrospectionSettings() {
        this(new Builder());
    }

    protected IntrospectionSettings(Builder b) {
        this.safeAccessCheck = b.safeAccessCheck;
        this.skipOnException = b.skipOnException != null ? b.skipOnException : (e) -> false;
        this.enterTargetType = b.enterTargetType;
        this.maxDepth = b.maxDepth;
        this.contentAccessor = b.contentAccessor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .withSafeAccessCheck(safeAccessCheck)
                .withSkipOnException(skipOnException)
                .withEnterTargetType(enterTargetType)
                .withMaxDepth(maxDepth)
                .withContentAccessor(contentAccessor);
    }

    public boolean useSafeAccessCheck() {
        return safeAccessCheck;
    }

    public boolean isEnterTargetType() {
        return enterTargetType;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public ContentAccessor getContentAccessor() {
        return contentAccessor;
    }

    public static class Builder {
        private boolean safeAccessCheck = true;
        @Nullable
        private Predicate<Exception> skipOnException = null;
        private boolean enterTargetType = true;
        private int maxDepth = Integer.MAX_VALUE;
        private ContentAccessor contentAccessor;

        public Builder() {
            this.contentAccessor = AccessorsChain.builder(true).build();
        }

        /**
         * If true (default), only fields and methods that may be accessed according to the rules will be made accessible.
         * If false, introspectors will throw an IllegalAccessException if faced with something it is not allowed to access.
         *
         * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/IllegalAccessException.html">Java 17 API: IllegalAccessException</a>
         */
        public Builder withSafeAccessCheck(boolean v) {
            this.safeAccessCheck = v;
            return this;
        }

        /**
         * If true (default), fields that cause any exception when accessed will be skipped.
         * If false, introspectors will throw an {@link systems.helius.reflet.exceptions.IntrospectionException}
         * if faced with an exception when accessing a field.
         *
         * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/IllegalAccessException.html">Java 17 API: IllegalAccessException</a>
         */
        public Builder withSkipOnException(Predicate<Exception> skipOnException) {
            this.skipOnException = skipOnException;
            return this;
        }

        /**
         * If true (default), instances of the target type will also be introspected for more instances.
         */
        public Builder withEnterTargetType(boolean enterTargetType) {
            this.enterTargetType = enterTargetType;
            return this;
        }

        /**
         * How deep in the root object to search for matches.
         */
        public Builder withMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder withContentAccessor(ContentAccessor contentAccessor) {
            this.contentAccessor = contentAccessor;
            return this;
        }

        public IntrospectionSettings build() {
            if (maxDepth < 0) throw new IllegalArgumentException("maxDepth must be >= 0");
            return new IntrospectionSettings(this);
        }
    }
}
