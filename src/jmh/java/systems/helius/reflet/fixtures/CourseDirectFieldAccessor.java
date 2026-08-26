package systems.helius.reflet.fixtures;

import systems.helius.reflet.reflection.IntrospectionContext;
import systems.helius.reflet.reflection.IntrospectionSettings;
import systems.helius.reflet.reflection.accessors.ChainComponentException;
import systems.helius.reflet.reflection.accessors.Content;
import systems.helius.reflet.reflection.accessors.ContentAccessor;

import jakarta.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link ContentAccessor} for {@link Course} objects that reads field values directly
 * by accessing the {@code public} fields of {@link Course}.
 *
 * <p>This accessor is the "direct field reads" variant: it bypasses any getters and
 * reads the public fields of {@code Course} without going through the reflection API
 * at call time.</p>
 */
public class CourseDirectFieldAccessor implements ContentAccessor {

    private static final Field TITLE_FIELD;
    private static final Field DESCRIPTION_FIELD;
    private static final Field TAGS_FIELD;
    private static final Field PREREQUISITES_FIELD;
    private static final Field GRADING_CRITERIA_FIELD;

    static {
        try {
            TITLE_FIELD = Course.class.getField("title");
            DESCRIPTION_FIELD = Course.class.getField("description");
            TAGS_FIELD = Course.class.getField("tags");
            PREREQUISITES_FIELD = Course.class.getField("prerequisites");
            GRADING_CRITERIA_FIELD = Course.class.getField("gradingCriteria");
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return Course.class.isAssignableFrom(current);
    }

    @Override
    public Collection<Content> extract(Object current,
                                       @Nullable Field holdingField,
                                       IntrospectionContext<?> context,
                                       IntrospectionSettings settings) throws ChainComponentException {
        Course course = (Course) current;
        List<Content> contents = new ArrayList<>(5);
        contents.add(new Content(course.title, TITLE_FIELD));
        contents.add(new Content(course.description, DESCRIPTION_FIELD));
        contents.add(new Content(course.tags, TAGS_FIELD));
        contents.add(new Content(course.prerequisites, PREREQUISITES_FIELD));
        contents.add(new Content(course.gradingCriteria, GRADING_CRITERIA_FIELD));
        return contents;
    }
}
