package systems.helius.reflet.fixtures;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ComplexChild extends ComplexHuman {
    private ComplexHuman parentA;
    private ComplexHuman parentB;
    private String favoriteToy;
    protected int canCountUpTo;

    ComplexChild(ComplexHuman base, ComplexHuman parentA, ComplexHuman parentB, String favoriteToy, int canCountUpTo) {
        super(base.getFirstName(), base.getMiddleName(), base.getLastName(), base.getAge(), base.getBirthSex(), base.getCurrentSex());
        this.parentA = parentA;
        this.parentB = parentB;
        this.favoriteToy = favoriteToy;
        this.canCountUpTo = canCountUpTo;
    }
}
