package ludot.random;

import java.util.List;
import java.util.Random;

/** Real RandomPicker implementation; the same seed always reproduces the same picks. */
public final class SeededPicker implements RandomPicker {

    private final Random random;

    public SeededPicker(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }
}
