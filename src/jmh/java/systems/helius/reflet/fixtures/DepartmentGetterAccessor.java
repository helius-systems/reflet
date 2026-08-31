package systems.helius.reflet.fixtures;

import systems.helius.reflet.IntrospectionContext;
import systems.helius.reflet.IntrospectionSettings;
import systems.helius.reflet.accessors.ChainComponentException;
import systems.helius.reflet.accessors.Content;
import systems.helius.reflet.accessors.ContentAccessor;

import jakarta.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link ContentAccessor} for {@link Department} objects that retrieves values by
 * calling its public getter methods directly.
 *
 * <p>This accessor is the "getter-based" variant: it calls the public getter methods on
 * {@code Department} rather than reading fields directly.</p>
 */
public class DepartmentGetterAccessor implements ContentAccessor {

    // Field references are used only for the Content wrappers (which require a Field).
    private static final Field NAME_FIELD;
    private static final Field STAFF_FIELD;
    private static final Field FOCUS_AREAS_FIELD;
    private static final Field COURSE_CATALOG_FIELD;

    static {
        try {
            NAME_FIELD = Department.class.getDeclaredField("name");
            STAFF_FIELD = Department.class.getDeclaredField("staff");
            FOCUS_AREAS_FIELD = Department.class.getDeclaredField("focusAreas");
            COURSE_CATALOG_FIELD = Department.class.getDeclaredField("courseCatalog");
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public boolean accepts(Class<?> current, @Nullable Field holdingField) {
        return Department.class.isAssignableFrom(current);
    }

    @Override
    public Collection<Content> extract(Object current,
                                       @Nullable Field holdingField,
                                       IntrospectionContext<?> context,
                                       IntrospectionSettings settings) throws ChainComponentException {
        Department dept = (Department) current;
        List<Content> contents = new ArrayList<>(4);
        contents.add(new Content(dept.getName(), NAME_FIELD));
        contents.add(new Content(dept.getStaff(), STAFF_FIELD));
        contents.add(new Content(dept.getFocusAreas(), FOCUS_AREAS_FIELD));
        contents.add(new Content(dept.getCourseCatalog(), COURSE_CATALOG_FIELD));
        return contents;
    }
}
