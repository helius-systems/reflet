package systems.helius.reflet.fixtures;

import com.sb.factorium.FakerGenerator;
import com.sb.factorium.GeneratorInfo;
import com.sb.factorium.RandomUtil;

import java.util.concurrent.ThreadLocalRandom;

@GeneratorInfo(name = "Base Human Generator", target = ComplexHuman.class, isDefault = true)
public class ComplexHumanGenerator extends FakerGenerator<ComplexHuman> {
    @Override
    protected ComplexHuman make() {
        Sex birthSex = RandomUtil.randomEnum(Sex.class);
        Sex currentSex = birthSex;
        if (Math.random() < 0.02)
            currentSex = RandomUtil.randomEnum(Sex.class);

        return new ComplexHuman(
                faker.name().firstName(),
                faker.name().prefix(),
                faker.name().lastName(),
                ThreadLocalRandom.current().nextInt(120),
                birthSex,
                currentSex
        );
    }
}
