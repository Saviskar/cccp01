package ludot.random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeededDiceTest {

    @Test
    @DisplayName("rolls are always between 1 and 6")
    void rollsAreAlwaysInRange() {
        Dice dice = new SeededDice(42L);
        for (int i = 0; i < 100; i++) {
            int roll = dice.roll();
            assertTrue(roll >= 1 && roll <= 6);
        }
    }

    @Test
    @DisplayName("the same seed always produces the same sequence of rolls")
    void sameSeedIsDeterministic() {
        Dice first = new SeededDice(7L);
        Dice second = new SeededDice(7L);
        for (int i = 0; i < 20; i++) {
            assertEquals(first.roll(), second.roll());
        }
    }

    @Test
    @DisplayName("a roll matches java.util.Random(seed).nextInt(6) + 1")
    void rollMatchesUnderlyingRandom() {
        Dice dice = new SeededDice(99L);
        Random expected = new Random(99L);
        for (int i = 0; i < 10; i++) {
            assertEquals(expected.nextInt(6) + 1, dice.roll());
        }
    }
}
