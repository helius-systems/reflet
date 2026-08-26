package systems.helius.reflet.fixtures;

import com.sb.factorium.FakerGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CourseGenerator extends FakerGenerator<Course> {

    @Override
    protected Course make() {
        return new Course(
                ThreadLocalRandom.current().nextInt(1000, 9999),
                faker.lorem().sentence(3),
                faker.lorem().paragraph()
        );
    }

    /**
     * Sets exactly {@code n} tags on the given course, replacing any existing tags.
     */
    public void setTags(Course course, int n) {
        List<String> tags = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            tags.add(faker.lorem().word());
        }
        course.setTags(tags);
    }

    /**
     * Sets exactly {@code n} prerequisite course names on the given course.
     */
    public void setPrerequisites(Course course, int n) {
        String[] prereqs = new String[n];
        for (int i = 0; i < n; i++) {
            prereqs[i] = faker.lorem().sentence(3);
        }
        course.setPrerequisites(prereqs);
    }

    /**
     * Populates the grading criteria map with exactly {@code n} entries.
     */
    public void setGradingCriteria(Course course, int n) {
        Map<String, Integer> criteria = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            criteria.put(faker.lorem().word() + i, faker.random().nextInt(1, 100));
        }
        course.setGradingCriteria(criteria);
    }

    /**
     * Generates a course and sets its multi-value fields to the given sizes.
     */
    public Course generateWithSizes(int nTags, int nPrerequisites, int nGradingCriteria) {
        Course course = generate();
        setTags(course, nTags);
        setPrerequisites(course, nPrerequisites);
        setGradingCriteria(course, nGradingCriteria);
        return course;
    }
}
