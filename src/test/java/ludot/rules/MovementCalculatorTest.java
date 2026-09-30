package ludot.rules;

import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovementCalculatorTest {

    private final BoardTopology topology = new BoardTopology();
    private final MovementCalculator calculator = new MovementCalculator();

    @Test
    @DisplayName("a1_rule1: a step within the standard track moves forward by the roll")
    void rule1_stepsForwardOnTrack() {
        RouteResult result = calculator.walk(new OnTrack(10), 4, Colour.RED, Direction.CLOCKWISE, topology);
        assertEquals(new RouteResult.Reachable(new OnTrack(14)), result);
    }

    @Test
    @DisplayName("A-06: landing exactly on Approach stays on the standard track")
    void a06_landingOnApproachStaysOnTrack() {
        int approach = topology.approachIndex(Colour.RED);
        RouteResult result = calculator.walk(new OnTrack(approach - 3), 3, Colour.RED, Direction.CLOCKWISE, topology);
        assertEquals(new RouteResult.Reachable(new OnTrack(approach)), result);
    }

    @Test
    @DisplayName("Rule 9: passing the Approach cell enters the home straight")
    void rule9_passingApproachEntersHomeStraight() {
        int approach = topology.approachIndex(Colour.RED);
        RouteResult result = calculator.walk(new OnTrack(approach), 2, Colour.RED, Direction.CLOCKWISE, topology);
        assertEquals(new RouteResult.Reachable(new InHomeStraight(1)), result);
    }

    @Test
    @DisplayName("Rule 10: the exact roll to reach Home lands AtHome")
    void rule10_exactRollReachesHome() {
        RouteResult result = calculator.walk(new InHomeStraight(3), 2, Colour.RED, Direction.CLOCKWISE, topology);
        assertEquals(new RouteResult.Reachable(new AtHome()), result);
    }

    @Test
    @DisplayName("A-09/Rule 10: overshooting Home is illegal, with no bounce-back")
    void a09_overshootingHomeIsIllegal() {
        RouteResult result = calculator.walk(new InHomeStraight(3), 3, Colour.RED, Direction.CLOCKWISE, topology);
        assertEquals(new RouteResult.Overshoot(), result);
    }

    @Test
    @DisplayName("a full lap plus the home straight is walked cell by cell without shortcuts")
    void walksMultipleCellsAcrossXAndApproach() {
        Colour colour = Colour.YELLOW;
        int x = topology.xIndex(colour);
        // 50 steps reaches Approach (A-05), then 6 more reaches Home: 56 in total (A-05).
        RouteResult result = calculator.walk(new OnTrack(x), 56, colour, Direction.CLOCKWISE, topology);
        assertEquals(new RouteResult.Reachable(new AtHome()), result);
    }

    @Test
    @DisplayName("counterclockwise walking is not implemented until phase 4a")
    void counterclockwiseNotYetSupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> calculator.walk(new OnTrack(10), 3, Colour.RED, Direction.COUNTERCLOCKWISE, topology));
    }
}
