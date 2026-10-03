package ludot.players;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.moves.BlockMove;
import ludot.moves.Move;
import ludot.moves.StepMove;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DESIGN.md §7: the shared "closest to home" ranking logic used by strategies via composition,
 * instead of a Template Method base class.
 */
class MoveRankingTest {

    private final MoveRanking ranking = new MoveRanking();
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    @Test
    @DisplayName("A-40: mover() returns the only piece id for a single-piece move")
    void a40_moverReturnsOnlyPieceForSinglePieceMove() {
        PieceId id = new PieceId(Colour.RED, 2);
        Move move = new StepMove(id, new OnTrack(5), new OnTrack(11), 6, Direction.CLOCKWISE, false, false, false, false, false);

        assertEquals(id, ranking.mover(move));
    }

    @Test
    @DisplayName("A-40/A-51: mover() returns the lowest-numbered piece in a block move")
    void a40_moverReturnsLowestNumberedPieceInBlockMove() {
        PieceId r3 = new PieceId(Colour.RED, 3);
        PieceId r1 = new PieceId(Colour.RED, 1);
        PieceId r2 = new PieceId(Colour.RED, 2);
        Move move = new BlockMove(
                List.of(r3, r1, r2), new OnTrack(5), new OnTrack(8), 6, 2, Direction.CLOCKWISE, false, false);

        assertEquals(r1, ranking.mover(move));
    }

    @Test
    @DisplayName("A-36/A-40: closestMoverToHome picks the move whose mover has the smallest distance")
    void a36_closestMoverToHomePicksSmallestDistance() {
        PieceId near = new PieceId(Colour.RED, 1);
        PieceId far = new PieceId(Colour.RED, 2);
        board.moveTo(near, new OnTrack(20));
        board.assignDirection(near, Direction.CLOCKWISE);
        board.moveTo(far, new OnTrack(5));
        board.assignDirection(far, Direction.CLOCKWISE);
        Move nearMove = new StepMove(near, new OnTrack(20), new OnTrack(26), 6, Direction.CLOCKWISE, false, false, false, false, false);
        Move farMove = new StepMove(far, new OnTrack(5), new OnTrack(11), 6, Direction.CLOCKWISE, false, false, false, false, false);

        assertEquals(nearMove, ranking.closestMoverToHome(List.of(farMove, nearMove), board));
    }

    @Test
    @DisplayName("A-40: closestMoverToHome ties break by the lowest piece number")
    void a40_closestMoverToHomeTieBreaksByLowestNumber() {
        PieceId r2 = new PieceId(Colour.RED, 2);
        PieceId r1 = new PieceId(Colour.RED, 1);
        board.moveTo(r2, new OnTrack(20));
        board.assignDirection(r2, Direction.CLOCKWISE);
        board.moveTo(r1, new OnTrack(20));
        board.assignDirection(r1, Direction.CLOCKWISE);
        Move moveForR2 = new StepMove(r2, new OnTrack(20), new OnTrack(26), 6, Direction.CLOCKWISE, false, false, false, false, false);
        Move moveForR1 = new StepMove(r1, new OnTrack(20), new OnTrack(26), 6, Direction.CLOCKWISE, false, false, false, false, false);

        assertEquals(moveForR1, ranking.closestMoverToHome(List.of(moveForR2, moveForR1), board));
    }
}
