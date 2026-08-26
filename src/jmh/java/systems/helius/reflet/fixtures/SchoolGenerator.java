package systems.helius.reflet.fixtures;

import com.sb.factorium.FakerGenerator;

import java.util.concurrent.ThreadLocalRandom;

public class SchoolGenerator extends FakerGenerator<School> {
    private static final ComplexHumanGenerator adultGenerator = new ComplexHumanGenerator();
    private static final HumanGenerator humanGenerator = new HumanGenerator();
    private final ClassroomGenerator classroomGenerator = new ClassroomGenerator();
    private final DepartmentGenerator departmentGenerator = new DepartmentGenerator();

    @Override
    protected School make() {
        var school = new School(faker.university().name(), faker.address().fullAddress());
        school.getTeachers().add(adultGenerator.generate());
        return school;
    }

    public void addTeachers(School school, int nTeachers) {
        for (ComplexHuman teacher : adultGenerator.generate(nTeachers)) {
            school.getTeachers().add(teacher);
        }
    }

    public void addStudents(School school, int nStudents) {
        for (ComplexHuman student : humanGenerator.generate(nStudents)) {
            StudentProfile profile = school.registerStudent(student);
            if (ThreadLocalRandom.current().nextFloat() > 0.25f) {
                profile.setAverage(ThreadLocalRandom.current().nextFloat());
            }
        }
    }

    /**
     * Adds exactly {@code n} classrooms (with default empty collections) to the school.
     */
    public void addClassrooms(School school, int n) {
        for (int i = 0; i < n; i++) {
            school.getClassrooms().add(classroomGenerator.generate());
        }
    }

    /**
     * Adds exactly {@code n} departments (with default empty collections) to the school.
     */
    public void addDepartments(School school, int n) {
        for (int i = 0; i < n; i++) {
            Department dept = departmentGenerator.generate();
            school.getDepartments().put(dept.getName(), dept);
        }
    }

    /**
     * Sets {@code n} consecutive semester years ending at the current year.
     */
    public void setSemesterYears(School school, int n) {
        int[] years = new int[n];
        int currentYear = java.time.Year.now().getValue();
        for (int i = 0; i < n; i++) {
            years[i] = currentYear - n + i + 1;
        }
        school.setSemesterYears(years);
    }
}
