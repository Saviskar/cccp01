package ludot.random;

import ludot.domain.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeededCoinTest {

    @Test
    @DisplayName("the same seed always produces the same sequence of tosses")
    void sameSeedIsDeterministic() {
        Coin first = new SeededCoin(3L);
        Coin second = new SeededCoin(3L);
        for (int i = 0; i < 20; i++) {
            assertEquals(first.toss(), second.toss());
        }
    }

    @Test
    @DisplayName("a toss matches java.util.Random(seed).nextBoolean()")
    void tossMatchesUnderlyingRandom() {
        Coin coin = new SeededCoin(11L);
        Random expected = new Random(11L);
        for (int i = 0; i < 10; i++) {
            Direction expectedDirection = expected.nextBoolean() ? Direction.CLOCKWISE : Direction.COUNTERCLOCKWISE;
            assertEquals(expectedDirection, coin.toss());
        }
    }
}
