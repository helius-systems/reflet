package systems.helius.reflet.fixtures;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Department {
    private String name;
    private List<ComplexHuman> staff;
    private String[] focusAreas;
    private Map<Integer, Course> courseCatalog;

    public Department(String name) {
        this.name = name;
        this.staff = new ArrayList<>();
        this.focusAreas = new String[0];
        this.courseCatalog = new HashMap<>();
    }
}
