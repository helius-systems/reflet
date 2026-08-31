package systems.helius.reflet.accessors;

import jakarta.annotation.Nullable;
import systems.helius.reflet.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A composite accessor that checks multiple accessors in order and uses the first one that accepts the current value.
 */
public class AccessorsChain implements ContentAccessor {
    private List<ContentAccessor> chain;
    /**
     * A cache of appropriate accessors for each class, to avoid repeatedly checking the chain for the same class.
     * The key is the class of the current value, and the value is the first accessor in the chain that accepts that class.
     * If no accessor accepts a class, it will not be added to the cache, and the chain will be checked again for that class.
     * This helps improve performance by avoiding redundant checks.
     */
    private Map<Class<?>, ContentAccessor> appropriate = new ConcurrentHashMap<>();

    protected AccessorsChain(AccessorsChain.Builder builder) {
        this.chain = new ArrayList<>(builder.chain.size() + (builder.lastResortEnabled ? 1 : 0));
        this.chain.addAll(builder.chain);
        if (builder.lastResortEnabled) {
            this.chain.add(builder.lastResortAccessor);
        }
    }

    /**
     * Construct a new accessors chain builder.
     *
     * @param withDefaults if true, add the following accessors:
     *                     <ol>
     *                     <li>{@link ArrayAccessor} checks each element of an array</li>
     *                     <li>{@link IterativeAccessor} checks each element of Iterables and does not inspect its fields.</li>
     *                     <li>{@link IterativeMapAccessor} check the key and value of each entry of Maps and does not check its fields.</li>
     *                     <li>{@link FieldHandlesAccessor} as the last resort accessor (checks all fields of all objects)</li>
     *                     </ol>
     */
    public static AccessorsChain.Builder builder(boolean withDefaults) {
        return new AccessorsChain.Builder(withDefaults);
    }

    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return chain.stream().anyMatch(chainElement -> chainElement.accepts(current, holdingField));
    }

    /**
     * Attempt to extract the content of the current object
     *
     * @param current      the current value to access the innards of.
     * @param holdingField the field that contained the current value.
     *                     Null when current is the root of the search.
     * @param context      the current introspection context
     * @param settings     settings of the current search
     * @return the content of the object
     * @throws ChainComponentException if a component of the chain throws an exception, it is thrown immediately if the exception does not allow for fallbacks.
     *                                 Otherwise, it is thrown only if none of the components managed to extract content and at least one threw an exception.
     */
    @Override
    public Collection<Content> extract(Object current, @Nullable Field holdingField, IntrospectionContext<?> context, IntrospectionSettings settings) throws ChainComponentException {
        ChainComponentException delayedException = null;
        Collection<Content> extracted = null;

        Class<?> targetClass = current.getClass();
        ContentAccessor accessor = this.appropriate.get(targetClass);
        if (accessor != null) {
            try {
                extracted = accessor.extract(current, holdingField, context, settings);
            } catch (ChainComponentException e) {
                if (!e.isAllowFallback()) {
                    throw e;
                }
                delayedException = e;
            }
        }
        if (extracted == null) {
            for (ContentAccessor chainElement : chain) {
                if (chainElement.accepts(targetClass, holdingField)) {
                    try {
                        extracted = chainElement.extract(current, holdingField, context, settings);
                        this.appropriate.put(targetClass, chainElement);
                        break;
                    } catch (ChainComponentException e) {
                        if (!e.isAllowFallback()) {
                            throw e;
                        }
                        delayedException = e;
                    }
                }
            }
        }
        if (extracted == null) { // If nothing is found
            if (delayedException != null) {
                // If we had a delayed exception, rethrow it
                throw delayedException;
            }
            return Collections.emptyList();
        }
        return extracted;
    }

    public static class Builder {
        protected final LinkedList<ContentAccessor> chain = new LinkedList<>();
        protected ClassInspector classInspector;
        protected FieldHandlesAccessor lastResortAccessor;
        protected boolean lastResortEnabled = false;

        /**
         * Construct a new accessor with the default built-in accessors.
         */
        public Builder() {
            this(true);
        }

        /**
         * Construct a new accessors chain builder.
         *
         * @param withDefaults if true, add the following accessors:
         *                     <ol>
         *                     <li>{@link ArrayAccessor} checks each element of an array</li>
         *                     <li>{@link IterativeAccessor} checks each element of Iterables and does not inspect its fields.</li>
         *                     <li>{@link IterativeMapAccessor} check the key and value of each entry of Maps and does not check its fields.</li>
         *                     <li>{@link FieldHandlesAccessor} as the last resort accessor (checks all fields of all objects)</li>
         *                     </ol>
         */
        public Builder(boolean withDefaults) {
            var lookupManager = new LookupManager();
            this.classInspector = new ClassInspector(lookupManager);
            this.lastResortAccessor = new FieldHandlesAccessor(new CachingClassInspector(lookupManager), lookupManager);
            if (withDefaults) {
                this.chain.add(new ArrayAccessor());
                this.chain.add(new IterativeAccessor());
                this.chain.add(new IterativeMapAccessor());
                this.lastResortEnabled = true;
            }
        }

        /**
         * Sets the ClassInspector to be used by all chain elements that require it.
         * This will replace the ClassInspector in all existing ClassInspectorAware elements in the chain with the new one.
         *
         * @param classInspector the ClassInspector to set
         * @return this builder for chaining
         * @throws IllegalStateException if any ClassInspectorAware element's replaceClassInspector method does not return a ContentAccessor
         */
        public Builder setClassInspector(ClassInspector classInspector) {
            this.classInspector = classInspector;

            // Update ClassInspectorAware content accessors
            ListIterator<ContentAccessor> iterator = chain.listIterator();
            while (iterator.hasNext()) {
                ContentAccessor chainElement = iterator.next();
                if (chainElement instanceof ClassInspectorAware<?> aware) {
                    Object replacement = aware.replaceClassInspector(classInspector);
                    if (!(replacement instanceof ContentAccessor)) {
                        throw new IllegalStateException(
                                "replaceClassInspector on " + chainElement.getClass().getName()
                                        + " returned " + (replacement == null ? "null" : replacement.getClass().getName())
                                        + ", which is not a ContentAccessor");
                    }
                    iterator.set((ContentAccessor) replacement);
                }
            }

            // Update the last resort accessor
            lastResortAccessor = lastResortAccessor.replaceClassInspector(classInspector);
            return this;
        }

        /**
         * Controls the presence of a special built-in accessor for iterable objects.
         *
         * @param iterate if true, instances of Iterable and arrays will be iterated over instead of being deeply inspected.
         *                If false, any instance of IterativeAccessor will be removed from this chain (if there already was one).
         * @return this builder.
         */
        public Builder iterateOverIterables(boolean iterate) {
            if (iterate) {
                replaceOrAddAtEnd(IterativeAccessor.class, new IterativeAccessor());
            } else {
                remove(IterativeAccessor.class);
            }
            return this;
        }

        public Builder iterateOverMapEntries(boolean iterate) {
            if (iterate) {
                replaceOrAddAtEnd(IterativeMapAccessor.class, new IterativeMapAccessor());
            } else {
                remove(IterativeMapAccessor.class);
            }
            return this;
        }

        public Builder iterateOverArrays(boolean iterate) {
            if (iterate) {
                replaceOrAddAtEnd(ArrayAccessor.class, new ArrayAccessor());
            } else {
                remove(ArrayAccessor.class);
            }
            return this;
        }

        /**
         * @param enable if true, an instance of {@link FieldHandlesAccessor} will be added at the end of the chain as a last resort accessor that accepts everything and attempts to access all fields using MethodHandles.
         *               if false, the built-in last resort accessor will be removed from the chain.
         * @return this builder
         */
        public Builder enableLastResortAccessor(boolean enable) {
            this.lastResortEnabled = enable;
            return this;
        }

        /**
         * Adds at the very beginning — highest priority.
         */
        public Builder addFirst(ContentAccessor accessor) {
            chain.addFirst(accessor);
            return this;
        }

        /**
         * Adds at the very end — lowest priority / last resort.
         * <p>
         * If defaults have been enabled or the {@link FieldHandlesAccessor} is enabled, the given argument
         * will be before the FieldsHandlesAccessor, since it accepts everything.
         */
        public Builder addLast(ContentAccessor accessor) {
            chain.addLast(accessor);
            return this;
        }

        /**
         * Inserts before the given type, or at the END if not found.
         */
        public Builder insertBefore(ContentAccessor accessor, Class<? extends ContentAccessor> beforeClass) {
            ListIterator<ContentAccessor> it = chain.listIterator();
            while (it.hasNext()) {
                if (it.next().getClass().equals(beforeClass)) {
                    it.previous();
                    it.add(accessor);
                    return this;
                }
            }
            chain.addLast(accessor); // fallback: add at end
            return this;
        }

        /**
         * Inserts after the given type, or at the END if not found.
         */
        public Builder insertAfter(ContentAccessor accessor, Class<? extends ContentAccessor> afterClass) {
            ListIterator<ContentAccessor> it = chain.listIterator();
            while (it.hasNext()) {
                if (it.next().getClass().equals(afterClass)) {
                    it.add(accessor);
                    return this;
                }
            }
            chain.addLast(accessor);
            return this;
        }

        /**
         * Swaps out an existing accessor for a new one (e.g. a decorated version).
         */
        public Builder replace(Class<? extends ContentAccessor> target, ContentAccessor replacement) {
            ListIterator<ContentAccessor> it = chain.listIterator();
            while (it.hasNext()) {
                if (it.next().getClass().equals(target)) {
                    it.set(replacement);
                    return this;
                }
            }
            throw new IllegalArgumentException("No accessor of type " + target.getSimpleName() + " found in chain");
        }

        /**
         * Swaps out an existing accessor for a new one (e.g. a decorated version),
         * or adds the new one at the end if no existing accessor of the target type is found.
         */
        public Builder replaceOrAddAtEnd(Class<? extends ContentAccessor> target, ContentAccessor replacement) {
            ListIterator<ContentAccessor> it = chain.listIterator();
            while (it.hasNext()) {
                if (it.next().getClass().equals(target)) {
                    it.set(replacement);
                    return this;
                }
            }
            chain.addLast(replacement);
            return this;
        }

        public boolean remove(Class<? extends ContentAccessor> accessorClass) {
            return chain.removeIf(a -> a.getClass().equals(accessorClass));
        }

        public AccessorsChain build() {
            return new AccessorsChain(this);
        }
    }
}
