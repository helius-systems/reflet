package systems.helius.reflet.fixtures;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class School {
    private final int ID;
    private String name;
    private String address;

    private Map<Integer, StudentProfile> students;
    private Set<ComplexHuman> teachers;
    private List<Classroom> classrooms;
    private Map<String, Department> departments;
    private int[] semesterYears;

    public School(String name, String address) {
        this(ThreadLocalRandom.current().nextInt(), name, address);
    }

    public School(int id, String name, String address) {
        this.ID = id;
        this.name = name;
        this.address = address;

        this.students = new HashMap<>();
        this.teachers = new LinkedHashSet<>();
        this.classrooms = new ArrayList<>();
        this.departments = new HashMap<>();
        this.semesterYears = new int[0];
    }

    public StudentProfile registerStudent(ComplexHuman student) {
        var profile = new StudentProfile(ThreadLocalRandom.current().nextInt(), student, this);
        this.students.put(profile.getStudentId(), profile);
        return profile;
    }
}