package systems.helius.reflet.fixtures;

import com.sb.factorium.FakerGenerator;

public class ClassroomGenerator extends FakerGenerator<Classroom> {

    private final CourseGenerator courseGenerator = new CourseGenerator();

    public CourseGenerator getCourseGenerator() {
        return courseGenerator;
    }

    @Override
    protected Classroom make() {
        return new Classroom(
                faker.letterify("Room ??-??"),
                faker.random().nextInt(20, 60)
        );
    }

    /**
     * Adds exactly {@code n} courses to the classroom.
     */
    public void addCourses(Classroom classroom, int n) {
        for (int i = 0; i < n; i++) {
            classroom.getCourses().add(courseGenerator.generate());
        }
    }

    /**
     * Replaces the facility-tags array with exactly {@code n} random tags.
     */
    public void setFacilityTags(Classroom classroom, int n) {
        String[] tags = new String[n];
        for (int i = 0; i < n; i++) {
            tags[i] = faker.lorem().word();
        }
        classroom.setFacilityTags(tags);
    }

    /**
     * Adds exactly {@code n} entries to the equipment map.
     */
    public void addEquipment(Classroom classroom, int n) {
        for (int i = 0; i < n; i++) {
            classroom.getEquipment().put(
                    faker.lorem().word() + classroom.getEquipment().size(),
                    faker.lorem().sentence());
        }
    }

    /**
     * Generates a classroom and sets its multi-value fields to the given sizes.
     */
    public Classroom generateWithSizes(int nCourses, int nFacilityTags, int nEquipment) {
        Classroom classroom = generate();
        addCourses(classroom, nCourses);
        setFacilityTags(classroom, nFacilityTags);
        addEquipment(classroom, nEquipment);
        return classroom;
    }
}
