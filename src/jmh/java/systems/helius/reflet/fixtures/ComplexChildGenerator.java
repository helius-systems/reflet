package systems.helius.reflet.fixtures;

import com.sb.factorium.FakerGenerator;

public class ComplexChildGenerator extends FakerGenerator<ComplexChild> {
    private ComplexHumanGenerator humanGenerator = new ComplexHumanGenerator();

    @Override
    protected ComplexChild make() {
        ComplexHuman base = humanGenerator.generate();
        ComplexHuman parentA = humanGenerator.generate();
        ComplexHuman parentB = humanGenerator.generate();
        return new ComplexChild(base, parentA, parentB, faker.letterify("??????"), faker.random().nextInt(0, 1000));
    }
}
