package systems.helius.reflet.fixtures;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Course {
    private int courseId;
    public String title;
    public String description;
    public List<String> tags;
    public String[] prerequisites;
    public Map<String, Integer> gradingCriteria;

    public Course(int courseId, String title, String description) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.tags = new ArrayList<>();
        this.prerequisites = new String[0];
        this.gradingCriteria = new HashMap<>();
    }
}
