package ludot.random;

import java.util.Random;

/** Real Dice implementation; the same seed always reproduces the same rolls. */
public final class SeededDice implements Dice {

    private static final int FACES = 6;

    private final Random random;

    public SeededDice(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public int roll() {
        return random.nextInt(FACES) + 1;
    }
}
