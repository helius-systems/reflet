package systems.helius.reflet.fixtures;

import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
public class Classroom {
    private String roomNumber;
    private int capacity;
    private Set<Course> courses;
    private String[] facilityTags;
    private Map<String, String> equipment;

    public Classroom(String roomNumber, int capacity) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.courses = new LinkedHashSet<>();
        this.facilityTags = new String[0];
        this.equipment = new HashMap<>();
    }
}
