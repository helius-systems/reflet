package systems.helius.reflet.reflection;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import systems.helius.reflet.exceptions.IntrospectionException;
import systems.helius.reflet.fixtures.Classroom;
import systems.helius.reflet.fixtures.ClassroomGenerator;
import systems.helius.reflet.fixtures.CourseGenerator;
import systems.helius.reflet.fixtures.Department;
import systems.helius.reflet.fixtures.DepartmentGenerator;
import systems.helius.reflet.fixtures.School;
import systems.helius.reflet.fixtures.SchoolGenerator;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks {@link BeanIntrospector#seek(Class, Object, java.lang.invoke.MethodHandles.Lookup)}
 * on one fixed-size school graph using a cached inspector.
 *
 * <p>The graph is built so that each multi-value field in the object graph is represented
 * at every one of the five canonical sizes: {@code 0} (empty), {@code 1} (single),
 * {@code 5}, {@code 15}, and {@code 30}. This ensures all four built-in accessors
 * ({@link systems.helius.reflet.reflection.accessors.ArrayAccessor},
 * {@link systems.helius.reflet.reflection.accessors.IterativeAccessor},
 * {@link systems.helius.reflet.reflection.accessors.IterativeMapAccessor}, and
 * {@link systems.helius.reflet.reflection.accessors.FieldHandlesAccessor}) are exercised
 * many times per traversal.</p>
 *
 * <h2>Multi-value field size distribution</h2>
 * <pre>
 * School-level
 *   semesterYears      : int[0]           (EMPTY)
 *   students           : 1 entry          (SINGLE)
 *   teachers           : 5 entries
 *   classrooms         : 5 Classrooms
 *   departments        : 5 Departments
 *
 * Classroom[i] (i = 0..4, sizes = {0, 1, 5, 15, 30})
 *   courses.size()     = SIZES[i]
 *   facilityTags.len   = SIZES[i]
 *   equipment.size()   = SIZES[i]
 *
 * Department[i] (i = 0..4, sizes = {0, 1, 5, 15, 30})
 *   staff.size()       = SIZES[i]
 *   focusAreas.len     = SIZES[i]
 *   courseCatalog.size()= SIZES[i]
 *
 * Courses inside Classroom[2] (5 courses, j = 0..4)
 *   tags.size()        = SIZES[j]
 *   prerequisites.len  = SIZES[j]
 *   gradingCriteria.sz = SIZES[j]
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 2)
@Warmup(time = 10, iterations = 5)
@Measurement(time = 20, iterations = 5)
public class BeanIntrospectorBenchmark {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * Five canonical sizes used to populate every multi-value field in the graph.
     */
    private static final int[] SIZES = {0, 1, 5, 15, 30};

    /**
     * Measures one full object-graph traversal searching for all reachable strings.
     *
     * @param inspector cached introspector benchmark state
     * @param plan      generated fixed-size input graph state
     * @param bh        JMH sink to prevent dead-code elimination
     * @throws IntrospectionException if fatal introspection access errors happen
     */
    @Benchmark
    public void seekAllReachableStrings(Caching inspector, ExecutionPlan plan, Blackhole bh)
            throws IntrospectionException {
        bh.consume(inspector.introspector.seek(String.class, plan.school, LOOKUP));
    }

    /**
     * Provides one fixed school object graph for each benchmark iteration.
     * This execution plan is meant to be worst-case for the introspector,
     * where the graph is very complex. This exposes more sharply
     * the performance of the BeanIntrospector and its components as a system.
     *
     * <p>The graph is constructed once per iteration via {@link #setupSchool()} and reused
     * across all benchmark invocations within that iteration.</p>
     */
    @State(Scope.Benchmark)
    public static class ExecutionPlan {
        private final SchoolGenerator schoolGenerator = new SchoolGenerator();
        private final ClassroomGenerator classroomGenerator = new ClassroomGenerator();
        private final CourseGenerator courseGenerator = new CourseGenerator();
        private final DepartmentGenerator departmentGenerator = new DepartmentGenerator();

        School school;

        /**
         * Builds the fixed-size graph once per iteration.
         *
         * <p>Each multi-value field in the graph is populated at every canonical size
         * (0, 1, 5, 15, 30) at least once across all instances of its containing class.</p>
         */
        @Setup(Level.Iteration)
        public void setupSchool() {
            school = schoolGenerator.generate();

            // Clear collections created by generate() so we control sizes precisely.
            school.getStudents().clear();
            school.getTeachers().clear();
            school.getClassrooms().clear();
            school.getDepartments().clear();

            // ── School-level multi-value fields ──────────────────────────────────────
            school.setSemesterYears(new int[0]);         // EMPTY  (size 0)
            schoolGenerator.addStudents(school, 1);      // SINGLE (size 1)
            schoolGenerator.addTeachers(school, 5);      // size 5

            // ── 5 Classrooms, one per canonical size ─────────────────────────────────
            for (int i = 0; i < SIZES.length; i++) {
                int size = SIZES[i];
                Classroom classroom = classroomGenerator.generateWithSizes(size, size, size);

                // Distribute Course-level sizes across the 5 courses in Classroom[2].
                if (i == 2) {
                    // Replace the uniformly-sized courses with ones covering all five sizes.
                    classroom.getCourses().clear();
                    for (int courseSize : SIZES) {
                        classroom.getCourses().add(courseGenerator.generateWithSizes(courseSize, courseSize, courseSize));
                    }
                }

                school.getClassrooms().add(classroom);
            }

            // ── 5 Departments, one per canonical size ────────────────────────────────
            for (int size : SIZES) {
                Department dept = departmentGenerator.generateWithSizes(size, size, size);
                school.getDepartments().put(dept.getName(), dept);
            }
        }
    }

    /**
     * Holds a cached {@link BeanIntrospector} instance for all benchmark invocations in a trial.
     */
    @State(Scope.Benchmark)
    public static class Caching {
        BeanIntrospector introspector;

        /**
         * Initializes the cached introspector once per trial.
         */
        @Setup(Level.Trial)
        public void initialize() {
            introspector = new BeanIntrospector(new CachingClassInspector());
        }
    }
}
