package ludot.board;

import ludot.domain.Colour;
import ludot.domain.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardTopologyTest {

    private final BoardTopology topology = new BoardTopology();

    @Test
    @DisplayName("A-01: the standard track has 52 cells")
    void a01_trackSizeIs52() {
        assertEquals(52, BoardTopology.TRACK_SIZE);
    }

    @Test
    @DisplayName("A-05: each home straight has 5 cells")
    void a05_homeStraightLengthIs5() {
        assertEquals(5, BoardTopology.HOME_STRAIGHT_LENGTH);
    }

    @Test
    @DisplayName("A-02: Yellow X = 0, Approach = 50")
    void a02_yellowXAndApproach() {
        assertEquals(0, topology.xIndex(Colour.YELLOW));
        assertEquals(50, topology.approachIndex(Colour.YELLOW));
    }

    @Test
    @DisplayName("A-02: Blue X = 13, Approach = 11")
    void a02_blueXAndApproach() {
        assertEquals(13, topology.xIndex(Colour.BLUE));
        assertEquals(11, topology.approachIndex(Colour.BLUE));
    }

    @Test
    @DisplayName("A-02: Red X = 26, Approach = 24")
    void a02_redXAndApproach() {
        assertEquals(26, topology.xIndex(Colour.RED));
        assertEquals(24, topology.approachIndex(Colour.RED));
    }

    @Test
    @DisplayName("A-02: Green X = 39, Approach = 37")
    void a02_greenXAndApproach() {
        assertEquals(39, topology.xIndex(Colour.GREEN));
        assertEquals(37, topology.approachIndex(Colour.GREEN));
    }

    @Test
    @DisplayName("A-03: Alpha = 7, Beta = 25, Gamma = 44, counted clockwise from the Yellow Approach cell")
    void a03_alphaBetaGammaIndices() {
        assertEquals(7, topology.alphaIndex());
        assertEquals(25, topology.betaIndex());
        assertEquals(44, topology.gammaIndex());
    }

    @Test
    @DisplayName("stepping clockwise wraps from the last cell (51) back to the first (0)")
    void stepClockwiseWrapsFromLastCellToFirst() {
        assertEquals(0, topology.step(51, Direction.CLOCKWISE));
    }

    @Test
    @DisplayName("stepping counterclockwise wraps from the first cell (0) back to the last (51)")
    void stepCounterclockwiseWrapsFromFirstCellToLast() {
        assertEquals(51, topology.step(0, Direction.COUNTERCLOCKWISE));
    }

    @Test
    @DisplayName("stepping moves exactly one cell in the requested direction")
    void stepMovesOneCellInEachDirection() {
        assertEquals(6, topology.step(5, Direction.CLOCKWISE));
        assertEquals(4, topology.step(5, Direction.COUNTERCLOCKWISE));
    }

    @Test
    @DisplayName("A-13: a clockwise piece's distance from home is steps-to-Approach plus 6")
    void a13_distanceFromHomeClockwise() {
        // Red Approach = 24 (A-02); 10 steps clockwise from cell 14 reaches it, then 6 more to Home.
        assertEquals(16, topology.distanceFromHome(Colour.RED, Direction.CLOCKWISE, 14, 0));
    }

    @Test
    @DisplayName("A-13/A-08: a counterclockwise piece past its first crossing needs one more lap before the real one")
    void a13_distanceFromHomeCounterclockwiseBeforeFirstCrossing() {
        // Red Approach = 24; sitting exactly on it with no crossings yet still needs a full
        // 52-cell lap before the crossing that counts (A-08), plus the final 6 to Home.
        assertEquals(58, topology.distanceFromHome(Colour.RED, Direction.COUNTERCLOCKWISE, 24, 0));
    }

    @Test
    @DisplayName("A-13/A-08: a counterclockwise piece on/after its first crossing is simply steps-to-Approach plus 6")
    void a13_distanceFromHomeCounterclockwiseAfterFirstCrossing() {
        // Red Approach = 24; sitting exactly on it with one crossing already made needs only the final 6.
        assertEquals(6, topology.distanceFromHome(Colour.RED, Direction.COUNTERCLOCKWISE, 24, 1));
    }
}
