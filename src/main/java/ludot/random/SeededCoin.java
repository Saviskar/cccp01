package ludot.random;

import ludot.domain.Direction;

import java.util.Random;

/** Real Coin implementation; the same seed always reproduces the same tosses. */
public final class SeededCoin implements Coin {

    private final Random random;

    public SeededCoin(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public Direction toss() {
        return random.nextBoolean() ? Direction.CLOCKWISE : Direction.COUNTERCLOCKWISE;
    }
}
