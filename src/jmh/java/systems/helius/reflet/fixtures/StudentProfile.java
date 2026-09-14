package systems.helius.reflet.fixtures;

import org.jspecify.annotations.Nullable;
import lombok.Data;
import lombok.Getter;

@Data
public class StudentProfile {
    @Getter(lombok.AccessLevel.NONE)
    protected final int studentId;
    protected ComplexHuman student;
    protected School school;
    @Nullable
    private Float average;

    protected StudentProfile(int studentId, ComplexHuman student, School school) {
        this.studentId = studentId;
        this.student = student;
        this.school = school;
    }

    public int getStudentId() {
        return studentId;
    }
}
