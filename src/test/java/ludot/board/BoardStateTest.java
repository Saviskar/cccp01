package ludot.board;

import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.NoEffect;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardStateTest {

    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    @Test
    @DisplayName("every colour starts with all 4 pieces in base")
    void everyColourStartsInBase() {
        for (Colour colour : Colour.values()) {
            assertEquals(4, board.countInBase(colour));
            assertEquals(0, board.countOnBoard(colour));
            assertEquals(0, board.countAtHome(colour));
        }
    }

    @Test
    @DisplayName("piece() returns the registered instance for a piece id")
    void pieceLooksUpRegisteredInstance() {
        PieceId id = new PieceId(Colour.RED, 1);
        Piece piece = board.piece(id);
        assertEquals(id, piece.id());
    }

    @Test
    @DisplayName("moveTo updates both the piece's own position and the reverse lookup")
    void moveToKeepsPieceAndOccupancyInSync() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(26));

        assertEquals(new OnTrack(26), board.piece(id).position());
        assertEquals(List.of(id), board.piecesAt(26));
        assertEquals(3, board.countInBase(Colour.RED));
        assertEquals(1, board.countOnBoard(Colour.RED));
    }

    @Test
    @DisplayName("A-14: two same-colour pieces on one track cell form a block")
    void a14_twoSameColourPiecesFormABlock() {
        PieceId first = new PieceId(Colour.GREEN, 1);
        PieceId second = new PieceId(Colour.GREEN, 2);
        board.moveTo(first, new OnTrack(5));
        board.moveTo(second, new OnTrack(5));

        assertTrue(board.isBlock(5));
        assertEquals(Optional.of(Colour.GREEN), board.colourAt(5));
        assertEquals(2, board.piecesAt(5).size());
    }

    @Test
    @DisplayName("moving one piece off a block cell un-blocks it")
    void movingAwayUnblocksTheCell() {
        PieceId first = new PieceId(Colour.GREEN, 1);
        PieceId second = new PieceId(Colour.GREEN, 2);
        board.moveTo(first, new OnTrack(5));
        board.moveTo(second, new OnTrack(5));

        board.moveTo(first, new OnTrack(6));

        assertFalse(board.isBlock(5));
        assertEquals(List.of(second), board.piecesAt(5));
    }

    @Test
    @DisplayName("A-10: same-colour pieces sharing a home-straight cell is not a block")
    void a10_homeStraightSharingIsNotABlock() {
        PieceId first = new PieceId(Colour.YELLOW, 1);
        PieceId second = new PieceId(Colour.YELLOW, 2);
        board.moveTo(first, new InHomeStraight(2));
        board.moveTo(second, new InHomeStraight(2));

        assertEquals(2, board.piecesInHomeStraight(Colour.YELLOW, 2).size());
        assertEquals(2, board.countOnBoard(Colour.YELLOW));
    }

    @Test
    @DisplayName("an empty track cell has no colour")
    void emptyCellHasNoColour() {
        assertEquals(Optional.empty(), board.colourAt(0));
    }

    @Test
    @DisplayName("A-26/T-9: resetToBase clears the piece's old track occupancy")
    void a26_resetToBaseClearsOldOccupancy() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));

        board.resetToBase(id);

        assertEquals(new InBase(), board.piece(id).position());
        assertEquals(List.of(), board.piecesAt(10));
        assertEquals(4, board.countInBase(Colour.RED));
    }

    @Test
    @DisplayName("recordCapture increments the piece's capture count")
    void recordCaptureIncrementsCaptureCount() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.recordCapture(id);
        assertEquals(1, board.piece(id).captureCount());
    }

    @Test
    @DisplayName("assignDirection sets the piece's original direction")
    void assignDirectionSetsOriginalDirection() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        assertEquals(Optional.of(Direction.COUNTERCLOCKWISE), board.piece(id).originalDirection());
    }

    @Test
    @DisplayName("recordApproachCrossing increments the piece's crossing counter")
    void recordApproachCrossingIncrementsCounter() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.recordApproachCrossing(id);
        assertEquals(1, board.piece(id).ccwApproachCrossings());
    }

    @Test
    @DisplayName("applyEffect replaces the piece's active effect")
    void applyEffectReplacesActiveEffect() {
        PieceId id = new PieceId(Colour.RED, 1);
        NoEffect effect = new NoEffect();
        board.applyEffect(id, effect);
        assertEquals(effect, board.piece(id).effect());
    }

    @Test
    @DisplayName("T-6: blockCellsOf returns every standard-track cell blocked by the given colour")
    void t6_blockCellsOfReturnsAllBlockCellsForColour() {
        board.moveTo(new PieceId(Colour.RED, 1), new OnTrack(5));
        board.moveTo(new PieceId(Colour.RED, 2), new OnTrack(5));
        board.moveTo(new PieceId(Colour.RED, 3), new OnTrack(20));
        board.moveTo(new PieceId(Colour.RED, 4), new OnTrack(20));

        assertEquals(List.of(5, 20), board.blockCellsOf(Colour.RED));
    }

    @Test
    @DisplayName("T-6: blockCellsOf ignores other colours and cells with fewer than 2 pieces")
    void t6_blockCellsOfIgnoresOtherColoursAndNonBlockCells() {
        board.moveTo(new PieceId(Colour.RED, 1), new OnTrack(5));
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(6));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(6));

        assertTrue(board.blockCellsOf(Colour.RED).isEmpty());
        assertEquals(List.of(6), board.blockCellsOf(Colour.GREEN));
    }
}
