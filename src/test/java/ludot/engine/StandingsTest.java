package ludot.engine;

import ludot.domain.Colour;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsTest {

    private static final List<Colour> ALL_COLOURS = List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE);

    @Test
    @DisplayName("A-41: the game is not over until 3 players have finished")
    void a41_notOverBeforeThreeFinishes() {
        Standings standings = new Standings();
        standings.recordFinish(Colour.RED);
        standings.recordFinish(Colour.GREEN);

        assertFalse(standings.isOver());

        standings.recordFinish(Colour.YELLOW);

        assertTrue(standings.isOver());
    }

    @Test
    @DisplayName("hasFinished reflects recorded finishes only")
    void hasFinishedReflectsRecordedFinishes() {
        Standings standings = new Standings();
        standings.recordFinish(Colour.RED);

        assertTrue(standings.hasFinished(Colour.RED));
        assertFalse(standings.hasFinished(Colour.GREEN));
    }

    @Test
    @DisplayName("A-41: the fourth place is assigned automatically once exactly 3 have finished")
    void a41_fourthPlaceAutoAssignedAtExactlyThree() {
        Standings standings = new Standings();
        standings.recordFinish(Colour.RED);
        standings.recordFinish(Colour.GREEN);
        standings.recordFinish(Colour.YELLOW);

        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE),
                standings.finalPlacings(ALL_COLOURS));
    }

    @Test
    @DisplayName("A-42: with fewer than 3 finishes (round guard), the partial order is returned unranked further")
    void a42_partialOrderPreservedUnderRoundGuard() {
        Standings standings = new Standings();
        standings.recordFinish(Colour.GREEN);

        assertEquals(List.of(Colour.GREEN), standings.finalPlacings(ALL_COLOURS));
    }
}
