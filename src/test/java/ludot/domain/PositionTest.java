package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    @Test
    @DisplayName("InBase and AtHome carry no data and are equal to themselves")
    void inBaseAndAtHomeAreSingularValues() {
        assertEquals(new InBase(), new InBase());
        assertEquals(new AtHome(), new AtHome());
    }

    @Test
    @DisplayName("A-01: OnTrack accepts indices 0 and 51")
    void onTrackAcceptsBoundaryIndices() {
        assertEquals(0, new OnTrack(0).index());
        assertEquals(51, new OnTrack(51).index());
    }

    @Test
    @DisplayName("A-01: OnTrack rejects index -1 and 52")
    void onTrackRejectsOutOfRangeIndices() {
        assertThrows(IllegalArgumentException.class, () -> new OnTrack(-1));
        assertThrows(IllegalArgumentException.class, () -> new OnTrack(52));
    }

    @Test
    @DisplayName("A-05: InHomeStraight accepts cells 0 and 4")
    void inHomeStraightAcceptsBoundaryCells() {
        assertEquals(0, new InHomeStraight(0).cell());
        assertEquals(4, new InHomeStraight(4).cell());
    }

    @Test
    @DisplayName("A-05: InHomeStraight rejects cell -1 and 5")
    void inHomeStraightRejectsOutOfRangeCells() {
        assertThrows(IllegalArgumentException.class, () -> new InHomeStraight(-1));
        assertThrows(IllegalArgumentException.class, () -> new InHomeStraight(5));
    }
}
