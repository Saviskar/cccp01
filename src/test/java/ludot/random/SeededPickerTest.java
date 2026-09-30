package ludot.random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeededPickerTest {

    @Test
    @DisplayName("picking from a single-element list always returns that element")
    void picksTheOnlyElement() {
        RandomPicker picker = new SeededPicker(5L);
        assertEquals("only", picker.pick(List.of("only")));
    }

    @Test
    @DisplayName("the same seed always produces the same sequence of picks")
    void sameSeedIsDeterministic() {
        List<String> options = List.of("a", "b", "c", "d");
        RandomPicker first = new SeededPicker(21L);
        RandomPicker second = new SeededPicker(21L);
        for (int i = 0; i < 20; i++) {
            assertEquals(first.pick(options), second.pick(options));
        }
    }

    @Test
    @DisplayName("a pick matches java.util.Random(seed).nextInt(size)")
    void pickMatchesUnderlyingRandom() {
        List<String> options = List.of("a", "b", "c", "d");
        RandomPicker picker = new SeededPicker(17L);
        Random expected = new Random(17L);
        for (int i = 0; i < 10; i++) {
            assertEquals(options.get(expected.nextInt(options.size())), picker.pick(options));
        }
    }
}
