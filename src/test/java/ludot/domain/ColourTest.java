package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColourTest {

    @Test
    @DisplayName("A-02: colour index order matches the X offset formula (Y=0, B=1, R=2, G=3)")
    void a02_colourIndexOrderMatchesOffsetFormula() {
        assertEquals(0, Colour.YELLOW.index());
        assertEquals(1, Colour.BLUE.index());
        assertEquals(2, Colour.RED.index());
        assertEquals(3, Colour.GREEN.index());
    }
}
