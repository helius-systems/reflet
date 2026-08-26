package systems.helius.reflet.fixtures;

import com.sb.factorium.FakerGenerator;

public class DepartmentGenerator extends FakerGenerator<Department> {

    private final ComplexHumanGenerator humanGenerator = new ComplexHumanGenerator();
    private final CourseGenerator courseGenerator = new CourseGenerator();

    @Override
    protected Department make() {
        return new Department(faker.lorem().word());
    }

    /**
     * Adds exactly {@code n} staff members to the department.
     */
    public void addStaff(Department department, int n) {
        for (ComplexHuman person : humanGenerator.generate(n)) {
            department.getStaff().add(person);
        }
    }

    /**
     * Replaces the focus-areas array with exactly {@code n} random area names.
     */
    public void setFocusAreas(Department department, int n) {
        String[] areas = new String[n];
        for (int i = 0; i < n; i++) {
            areas[i] = faker.lorem().word();
        }
        department.setFocusAreas(areas);
    }

    /**
     * Adds exactly {@code n} courses to the department's course catalog.
     */
    public void addCoursesToCatalog(Department department, int n) {
        for (int i = 0; i < n; i++) {
            Course course = courseGenerator.generate();
            department.getCourseCatalog().put(course.getCourseId(), course);
        }
    }

    /**
     * Generates a department and sets its multi-value fields to the given sizes.
     */
    public Department generateWithSizes(int nStaff, int nFocusAreas, int nCourses) {
        Department dept = generate();
        addStaff(dept, nStaff);
        setFocusAreas(dept, nFocusAreas);
        addCoursesToCatalog(dept, nCourses);
        return dept;
    }
}
