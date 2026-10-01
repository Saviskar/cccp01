package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementCalculatorTest {

    private final BoardTopology topology = new BoardTopology();
    private final MovementCalculator calculator = new MovementCalculator();
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    @Test
    @DisplayName("a1_rule1: a step within the standard track moves forward by the roll")
    void rule1_stepsForwardOnTrack() {
        RouteResult result =
                calculator.walk(new OnTrack(10), 4, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(14), false), result);
    }

    @Test
    @DisplayName("A-06: landing exactly on Approach stays on the standard track")
    void a06_landingOnApproachStaysOnTrack() {
        int approach = topology.approachIndex(Colour.RED);
        RouteResult result = calculator.walk(
                new OnTrack(approach - 3), 3, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(approach), false), result);
    }

    @Test
    @DisplayName("Rule 9: passing the Approach cell enters the home straight")
    void rule9_passingApproachEntersHomeStraight() {
        int approach = topology.approachIndex(Colour.RED);
        // A-07: eligible (captureCount >= 1) — this test is about Rule 9, not T-7.
        RouteResult result =
                calculator.walk(new OnTrack(approach), 2, Colour.RED, Direction.CLOCKWISE, 0, 1, topology, board);
        assertEquals(new RouteResult.Reachable(new InHomeStraight(1), false), result);
    }

    @Test
    @DisplayName("Rule 10: the exact roll to reach Home lands AtHome")
    void rule10_exactRollReachesHome() {
        RouteResult result =
                calculator.walk(new InHomeStraight(3), 2, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new AtHome(), false), result);
    }

    @Test
    @DisplayName("A-09/Rule 10: overshooting Home is illegal, with no bounce-back")
    void a09_overshootingHomeIsIllegal() {
        RouteResult result =
                calculator.walk(new InHomeStraight(3), 3, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Overshoot(), result);
    }

    @Test
    @DisplayName("a full lap plus the home straight is walked cell by cell without shortcuts")
    void walksMultipleCellsAcrossXAndApproach() {
        Colour colour = Colour.YELLOW;
        int x = topology.xIndex(colour);
        // 50 steps reaches Approach (A-05), then 6 more reaches Home: 56 in total (A-05).
        // A-07: eligible (captureCount >= 1) — this test is about Rule 9/A-05, not T-7.
        RouteResult result = calculator.walk(new OnTrack(x), 56, colour, Direction.CLOCKWISE, 0, 1, topology, board);
        assertEquals(new RouteResult.Reachable(new AtHome(), false), result);
    }

    @Test
    @DisplayName("T-1: a counterclockwise step decrements the track index")
    void t1_stepsBackwardWhenCounterclockwise() {
        RouteResult result =
                calculator.walk(new OnTrack(10), 3, Colour.RED, Direction.COUNTERCLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(7), false), result);
    }

    @Test
    @DisplayName("A-08: the first counterclockwise Approach crossing continues on the standard track")
    void a08_firstApproachCrossingContinuesOnTrack() {
        Colour colour = Colour.RED;
        int approach = topology.approachIndex(colour);
        RouteResult result =
                calculator.walk(new OnTrack(approach), 1, colour, Direction.COUNTERCLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(topology.step(approach, Direction.COUNTERCLOCKWISE)), true),
                result);
    }

    @Test
    @DisplayName("A-06/A-08: landing exactly on Approach counterclockwise stays on track regardless of crossings")
    void a08_landingExactlyOnApproachStaysOnTrack() {
        Colour colour = Colour.RED;
        int approach = topology.approachIndex(colour);
        int start = topology.step(approach, Direction.CLOCKWISE); // one cell before Approach, moving CCW
        RouteResult result =
                calculator.walk(new OnTrack(start), 1, colour, Direction.COUNTERCLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(approach), false), result);
    }

    @Test
    @DisplayName("A-08: the second counterclockwise Approach crossing enters the home straight")
    void a08_secondCrossingEntersHomeStraight() {
        Colour colour = Colour.RED;
        int approach = topology.approachIndex(colour);
        // A-07: eligible (captureCount >= 1) — this test is about A-08's crossing count, not T-7.
        RouteResult result =
                calculator.walk(new OnTrack(approach), 1, colour, Direction.COUNTERCLOCKWISE, 1, 1, topology, board);
        assertEquals(new RouteResult.Reachable(new InHomeStraight(0), false), result);
    }

    @Test
    @DisplayName("A-08: a full counterclockwise route is 2 + 52 + 6 = 60 steps")
    void a08_fullCounterclockwiseRouteIsSixtySteps() {
        Colour colour = Colour.YELLOW;
        int x = topology.xIndex(colour);
        // A-07: eligible (captureCount >= 1) — this test is about A-08's full route length, not T-7.
        RouteResult result =
                calculator.walk(new OnTrack(x), 60, colour, Direction.COUNTERCLOCKWISE, 0, 1, topology, board);
        assertEquals(new RouteResult.Reachable(new AtHome(), true), result);
    }

    @Test
    @DisplayName("Rule 5: a single opponent piece along the path does not obstruct")
    void rule5_singleOpponentDoesNotObstruct() {
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(12));

        RouteResult result =
                calculator.walk(new OnTrack(10), 4, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);

        assertEquals(new RouteResult.Reachable(new OnTrack(14), false), result);
    }

    @Test
    @DisplayName("A-15: an own-colour block does not obstruct")
    void a15_ownColourBlockDoesNotObstruct() {
        board.moveTo(new PieceId(Colour.RED, 2), new OnTrack(12));
        board.moveTo(new PieceId(Colour.RED, 3), new OnTrack(12));

        RouteResult result =
                calculator.walk(new OnTrack(10), 4, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);

        assertEquals(new RouteResult.Reachable(new OnTrack(14), false), result);
    }

    @Test
    @DisplayName("A-16: an opponent block directly ahead obstructs, with 0 cells of partial movement")
    void a16_opponentBlockObstructsAdjacent() {
        PieceId blocker1 = new PieceId(Colour.GREEN, 1);
        PieceId blocker2 = new PieceId(Colour.GREEN, 2);
        board.moveTo(blocker1, new OnTrack(11));
        board.moveTo(blocker2, new OnTrack(11));

        RouteResult result =
                calculator.walk(new OnTrack(10), 4, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);

        assertEquals(
                new RouteResult.Obstructed(new OnTrack(10), 0, false, new OnTrack(14), blocker1), result);
    }

    @Test
    @DisplayName("T-3 worked example: G1 at 0, an R block at 4, rolling 6 stops at cell 3")
    void t3_workedExampleObstructedPartwayStopsBeforeBlock() {
        PieceId r1 = new PieceId(Colour.RED, 1);
        PieceId r2 = new PieceId(Colour.RED, 2);
        board.moveTo(r1, new OnTrack(4));
        board.moveTo(r2, new OnTrack(4));

        RouteResult result =
                calculator.walk(new OnTrack(0), 6, Colour.GREEN, Direction.CLOCKWISE, 0, 0, topology, board);

        assertEquals(new RouteResult.Obstructed(new OnTrack(3), 3, false, new OnTrack(6), r1), result);
    }

    @Test
    @DisplayName("T-3: obstruction also stops a counterclockwise walk before the block")
    void t3_counterclockwiseObstructionStopsBeforeBlock() {
        PieceId r1 = new PieceId(Colour.RED, 1);
        PieceId r2 = new PieceId(Colour.RED, 2);
        board.moveTo(r1, new OnTrack(6));
        board.moveTo(r2, new OnTrack(6));

        RouteResult result =
                calculator.walk(new OnTrack(10), 6, Colour.GREEN, Direction.COUNTERCLOCKWISE, 0, 0, topology, board);

        assertEquals(new RouteResult.Obstructed(new OnTrack(7), 3, false, new OnTrack(4), r1), result);
    }

    @Test
    @DisplayName("A-49: an obstruction found early in the walk is superseded by a later overshoot")
    void a49_overshootBeatsObstruction() {
        // Approach for RED is cell 24 (A-02); starting 5 cells before it and rolling 12 passes
        // an opponent block on the way, then reaches Home exactly at step 11 and overshoots on
        // step 12 — the overshoot must win, since the move is illegal either way.
        int approach = topology.approachIndex(Colour.RED);
        int origin = approach - 5;
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(origin + 2));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(origin + 2));

        // A-07: eligible (captureCount >= 1) — otherwise the piece can't reach Home to overshoot it.
        RouteResult result =
                calculator.walk(new OnTrack(origin), 12, Colour.RED, Direction.CLOCKWISE, 0, 1, topology, board);

        assertEquals(new RouteResult.Overshoot(), result);
    }

    @Test
    @DisplayName("A-07/A-08: a clockwise piece with no captures continues past the Approach without "
            + "entering, and records no Approach crossing (A-08 is counterclockwise-only)")
    void a07_ineligiblePieceContinuesPastApproachClockwise() {
        int approach = topology.approachIndex(Colour.RED);
        RouteResult result =
                calculator.walk(new OnTrack(approach), 1, Colour.RED, Direction.CLOCKWISE, 0, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(topology.step(approach, Direction.CLOCKWISE)), false),
                result);
    }

    @Test
    @DisplayName("A-07: a counterclockwise piece on its second crossing still continues without a capture, "
            + "and still records the crossing (A-08 is independent of A-07)")
    void a07_ineligibleCounterclockwisePieceStillContinuesOnSecondCrossing() {
        Colour colour = Colour.RED;
        int approach = topology.approachIndex(colour);
        RouteResult result =
                calculator.walk(new OnTrack(approach), 1, colour, Direction.COUNTERCLOCKWISE, 1, 0, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(topology.step(approach, Direction.COUNTERCLOCKWISE)), true),
                result);
    }

    @Test
    @DisplayName("A-07/A-08: a counterclockwise piece with a prior capture is still blocked by its first "
            + "crossing (A-08's crossing-count gate is independent of A-07's capture gate)")
    void a07_eligiblePieceStillBlockedByFirstCrossing() {
        Colour colour = Colour.RED;
        int approach = topology.approachIndex(colour);
        RouteResult result =
                calculator.walk(new OnTrack(approach), 1, colour, Direction.COUNTERCLOCKWISE, 0, 1, topology, board);
        assertEquals(new RouteResult.Reachable(new OnTrack(topology.step(approach, Direction.COUNTERCLOCKWISE)), true),
                result);
    }

    @Test
    @DisplayName("T-7/A-07: a piece with at least one capture enters the home straight on first pass")
    void t7_a07_eligiblePieceEntersHomeStraightOnFirstPass() {
        int approach = topology.approachIndex(Colour.RED);
        RouteResult result =
                calculator.walk(new OnTrack(approach), 1, Colour.RED, Direction.CLOCKWISE, 0, 1, topology, board);
        assertEquals(new RouteResult.Reachable(new InHomeStraight(0), false), result);
    }
}
