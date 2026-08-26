package systems.helius.reflet.fixtures;

import com.sb.factorium.BaseGenerator;
import com.sb.factorium.GeneratorInfo;

import java.util.concurrent.ThreadLocalRandom;

@GeneratorInfo(name = "Universal Human Generator", target = ComplexHuman.class, isDefault = false)
public class HumanGenerator extends BaseGenerator<ComplexHuman> {
    private static ComplexHumanGenerator complexHumanGenerator = new ComplexHumanGenerator();
    private static ComplexChildGenerator complexChildGenerator = new ComplexChildGenerator();

    @Override
    protected ComplexHuman make() {
        if (ThreadLocalRandom.current().nextBoolean()) {
            return complexHumanGenerator.generate();
        } else {
            return complexChildGenerator.generate();
        }
    }
}
