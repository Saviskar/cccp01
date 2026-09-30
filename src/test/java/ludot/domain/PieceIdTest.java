package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PieceIdTest {

    @Test
    @DisplayName("piece numbers 1-4 are accepted")
    void acceptsValidNumbers() {
        for (int number = 1; number <= 4; number++) {
            int finalNumber = number;
            assertDoesNotThrow(() -> new PieceId(Colour.RED, finalNumber));
        }
    }

    @Test
    @DisplayName("piece number 0 is rejected")
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new PieceId(Colour.RED, 0));
    }

    @Test
    @DisplayName("piece number 5 is rejected")
    void rejectsFive() {
        assertThrows(IllegalArgumentException.class, () -> new PieceId(Colour.RED, 5));
    }

    @Test
    @DisplayName("two PieceIds with the same colour and number are equal")
    void equalityIsByValue() {
        assertEquals(new PieceId(Colour.GREEN, 2), new PieceId(Colour.GREEN, 2));
    }
}
