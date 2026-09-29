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
}
