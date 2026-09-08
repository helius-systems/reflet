package systems.helius.reflet;

import systems.helius.reflet.accessors.AccessorsChain;
import systems.helius.reflet.accessors.ContentAccessor;
import systems.helius.reflet.exceptions.IntrospectionFailureHandler;

/**
 * Configuration applied to a {@link BeanIntrospector} search.
 */
public class IntrospectionSettings {
    /**
     * Policy used only when a class or member cannot be made accessible.
     */
    protected final AccessDenialPolicy accessDenialPolicy;

    /**
     * Policy used for thrown extraction and descent exceptions.
     */
    protected final IntrospectionFailureHandler exceptionHandler;

    /**
     * If true (default), instances of the target type will also be introspected for more instances.
     */
    protected final boolean enterTargetType;

    /**
     * How deep in the root object to search for matches.
     */
    protected final int maxDepth;

    /**
     * Accessor used to extract child content from each visited object.
     */
    protected final ContentAccessor contentAccessor;

    /**
     * Default introspection settings.
     */
    public IntrospectionSettings() {
        this(new Builder());
    }

    /**
     * Creates settings from a builder.
     *
     * @param b the builder containing the desired settings.
     */
    protected IntrospectionSettings(Builder b) {
        this.accessDenialPolicy = b.accessDenialPolicy;
        this.exceptionHandler = b.exceptionHandler;
        this.enterTargetType = b.enterTargetType;
        this.maxDepth = b.maxDepth;
        this.contentAccessor = b.contentAccessor;
    }

    /**
     * Creates a new settings builder.
     *
     * @return a builder initialized with default settings.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Copies these settings into a mutable builder.
     *
     * @return a builder initialized from this instance.
     */
    public Builder toBuilder() {
        return new Builder()
                .withAccessDenialPolicy(accessDenialPolicy)
                .withExceptionHandler(exceptionHandler)
                .withEnterTargetType(enterTargetType)
                .withMaxDepth(maxDepth)
                .withContentAccessor(contentAccessor);
    }

    /**
     * Returns the policy used for inaccessible classes and members.
     *
     * @return the configured access-denial policy.
     */
    public AccessDenialPolicy getAccessDenialPolicy() {
        return accessDenialPolicy;
    }

    /**
     * Returns the policy used for thrown extraction and descent exceptions.
     *
     * @return the configured exception handler.
     */
    public IntrospectionFailureHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Returns whether matching target values should also be recursively searched.
     *
     * @return {@code true} when matching values are entered.
     */
    public boolean isEnterTargetType() {
        return enterTargetType;
    }

    /**
     * Returns the maximum search depth.
     *
     * @return the maximum depth.
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Returns the content accessor used by searches.
     *
     * @return the configured content accessor.
     */
    public ContentAccessor getContentAccessor() {
        return contentAccessor;
    }

    /**
     * Builder for {@link IntrospectionSettings}.
     */
    public static class Builder {
        private AccessDenialPolicy accessDenialPolicy = AccessDenialPolicy.SKIP;
        private IntrospectionFailureHandler exceptionHandler = IntrospectionFailureHandler.propagateAll();
        private boolean enterTargetType = true;
        private int maxDepth = Integer.MAX_VALUE;
        private ContentAccessor contentAccessor;

        /**
         * Creates a builder initialized with the default accessor chain.
         */
        public Builder() {
            this.contentAccessor = AccessorsChain.builder(true).build();
        }

        /**
         * Sets the policy used only when a class or member cannot be made accessible.
         *
         * @param accessDenialPolicy the access-denial policy.
         * @return this builder.
         * @throws NullPointerException if {@code accessDenialPolicy} is {@code null}.
         */
        public Builder withAccessDenialPolicy(AccessDenialPolicy accessDenialPolicy) {
            this.accessDenialPolicy = accessDenialPolicy;
            return this;
        }

        /**
         * Sets the policy used for thrown extraction and descent exceptions.
         *
         * @param exceptionHandler the exception handler.
         * @return this builder.
         * @throws NullPointerException if {@code exceptionHandler} is {@code null}.
         */
        public Builder withExceptionHandler(IntrospectionFailureHandler exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * If true (default), instances of the target type will also be introspected for more instances.
         *
         * @param enterTargetType whether target-type matches should also be entered.
         * @return this builder.
         */
        public Builder withEnterTargetType(boolean enterTargetType) {
            this.enterTargetType = enterTargetType;
            return this;
        }

        /**
         * How deep in the root object to search for matches.
         *
         * @param maxDepth the maximum search depth.
         * @return this builder.
         */
        public Builder withMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Sets the content accessor used by introspection.
         *
         * @param contentAccessor the content accessor.
         * @return this builder.
         * @throws NullPointerException if {@code contentAccessor} is {@code null}.
         */
        public Builder withContentAccessor(ContentAccessor contentAccessor) {
            this.contentAccessor = contentAccessor;
            return this;
        }

        /**
         * Builds the settings.
         *
         * @return a new settings instance.
         * @throws IllegalArgumentException if {@code maxDepth} is negative.
         */
        public IntrospectionSettings build() {
            if (maxDepth < 0) throw new IllegalArgumentException("maxDepth must be >= 0");
            return new IntrospectionSettings(this);
        }
    }
}
