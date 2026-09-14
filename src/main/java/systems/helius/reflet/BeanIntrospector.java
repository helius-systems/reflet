package systems.helius.reflet;

import jakarta.annotation.Nullable;
import systems.helius.reflet.exceptions.*;

import systems.helius.reflet.accessors.Content;

import java.lang.reflect.Field;
import java.util.*;

import static java.lang.invoke.MethodHandles.Lookup;

public class BeanIntrospector {
    protected final IntrospectionSettings settings;

    public BeanIntrospector() {
        this(null);
    }

    public BeanIntrospector(IntrospectionSettings settings) {
        this.settings = settings != null ? settings : new IntrospectionSettings();
    }


    /**
     * Seek within the root and all children for instances of a given type.
     * Warning! The returned set uses object identity (==), not equals() as is usually the case with sets.
     * @param targetType instances to find must be of that type or a covalent type.
     * @param root seek into
     * @param context the context of the caller. Should always be MethodHandles.lookup();
     * @return every instance found of the given type
     * @throws IntrospectionException if any fatal access issues are encountered during the introspection.
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/IdentityHashMap.html">Java 17 API: IdentitHashMap</a>
     */
    public <T> Set<T> seek(Class<T> targetType, Object root, Lookup context) throws IntrospectionException {
        Set<T> found = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            depthFirstSearch(root,
                    null,
                    0,
                    new IntrospectionContext<>(targetType, context, found, visited, settings.getContentAccessor()),
                    settings);
        } catch (TracedAccessException e) {
            e.setRoot(root);
            throw new IntrospectionException(e);
        }
        return found;
    }

    @SuppressWarnings("unchecked") // unchecked cast to T covered by the static isAssignableFrom check
    protected <T> void depthFirstSearch(Object current,
                                        @Nullable Field holdingField,
                                        int depth,
                                        IntrospectionContext<T> context,
                                        IntrospectionSettings settings) throws TracedAccessException {
        // Checks
        if (current == null || depth >= settings.getMaxDepth() || context.visited().contains(current))
            return;
        context.visited().add(current);

        // Check if the current object is what we are looking for
        if (ClassInspector.evaluateTypingMatch(context.targetType(), current, (holdingField != null ? holdingField.getType() : null))) {
            context.found().add((T) current);
            if (!settings.isEnterTargetType())
                return;
        }

        if (TypeUtils.isPrimitiveWrapper(current.getClass())) // Also catches primitives due to the autoboxing
            return;
        // End of checks

        descendInto(current, holdingField, depth, context, settings);
    }

    protected <T> void descendInto(Object current, Field holdingField, int depth, IntrospectionContext<T> context, IntrospectionSettings settings) throws TracedAccessException {
        Collection<Content> content;
        try {
            content = context.contentAccessor().extract(current, holdingField, context, settings);
        } catch (AccessorException | RuntimeException e) {
            if (e instanceof AccessorException ae && ae.isFatal()) {
                var traced = new TracedAccessException(e);
                traced.addStep(holdingField);
                throw traced;
            }
            var exceptionContext = new ExceptionContext(e, current, holdingField, context.targetType(), ExceptionContext.Origin.DESCENT);
            ExceptionResolution resolution = settings.getExceptionHandler().handle(exceptionContext);
            if (resolution instanceof ExceptionResolution.Skip) {
                content = Collections.emptyList();
            } else if (resolution instanceof ExceptionResolution.Substitute substitute) {
                content = substitute.value();
            } else { // Propagate
                var traced = new TracedAccessException(e);
                traced.addStep(holdingField);
                throw traced;
            }
        }
        if (content == null || content.isEmpty()) {
            return;
        }

        final int currentDepth = depth + 1;
        for (Content c : content) {
            if (c == null) continue;
            try {
                depthFirstSearch(c.value(), c.holdingField(), currentDepth, context, settings);
            } catch (TracedAccessException e) {
                e.addStep(holdingField);
                throw e;
            }
        }
    }
}
