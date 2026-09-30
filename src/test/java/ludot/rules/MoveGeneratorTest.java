package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.moves.Move;
import ludot.moves.MoveContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveGeneratorTest {

    private final BoardTopology topology = new BoardTopology();
    private final MoveGenerator generator = new MoveGenerator(new MovementCalculator());
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    @Test
    @DisplayName("Rule 2/3: with everything in base, only a six produces a legal move")
    void rule2_onlyASixLeavesBase() {
        assertTrue(generator.legalMoves(Colour.RED, 5, board, topology).isEmpty());
        assertEquals(4, generator.legalMoves(Colour.RED, 6, board, topology).size());
    }

    @Test
    @DisplayName("a piece already on the track produces one step move per roll")
    void onTrackPieceProducesOneStepMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);

        List<Move> moves = generator.legalMoves(Colour.RED, 4, board, topology);

        assertEquals(1, moves.size());
        Move move = moves.get(0);
        assertEquals(id, move.pieceId());
        assertEquals(new OnTrack(14), move.destination());
    }

    @Test
    @DisplayName("Rule 7: landing on an own-colour standard-track cell is illegal")
    void rule7_ownColourTrackLandingIsIllegal() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId blocker = new PieceId(Colour.RED, 2);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(blocker, new OnTrack(14));
        board.assignDirection(blocker, Direction.CLOCKWISE);

        List<Move> moves = generator.legalMoves(Colour.RED, 4, board, topology);

        assertTrue(moves.stream().noneMatch(m -> m.pieceId().equals(mover)));
    }

    @Test
    @DisplayName("A-10: own-colour sharing in the home straight is legal, unlike on the track")
    void a10_ownColourHomeStraightLandingIsLegal() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId sibling = new PieceId(Colour.RED, 2);
        board.moveTo(mover, new InHomeStraight(1));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(sibling, new InHomeStraight(2));
        board.assignDirection(sibling, Direction.CLOCKWISE);

        List<Move> moves = generator.legalMoves(Colour.RED, 1, board, topology);

        assertTrue(moves.stream().anyMatch(m -> m.pieceId().equals(mover)
                && m.destination().equals(new InHomeStraight(2))));
    }

    @Test
    @DisplayName("Rule 10: an overshooting move is not generated")
    void rule10_overshootingMoveIsNotGenerated() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new InHomeStraight(3));
        board.assignDirection(id, Direction.CLOCKWISE);

        List<Move> moves = generator.legalMoves(Colour.RED, 3, board, topology);

        assertTrue(moves.isEmpty());
    }

    @Test
    @DisplayName("Rule 6: a move landing on a single opponent flags capturesSomething")
    void rule6_landingOnSingleOpponentFlagsCapture() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(14));

        List<Move> moves = generator.legalMoves(Colour.RED, 4, board, topology);

        assertEquals(1, moves.size());
        assertTrue(moves.get(0).capturesSomething());
    }

    @Test
    @DisplayName("entering base to X onto a single opponent flags capturesSomething")
    void enteringXOntoSingleOpponentFlagsCapture() {
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.xIndex(Colour.RED)));

        List<Move> moves = generator.legalMoves(Colour.RED, 6, board, topology);

        assertEquals(4, moves.size());
        assertTrue(moves.stream().allMatch(Move::capturesSomething));
    }

    @Test
    @DisplayName("Rule 7: entering base to X onto an own-colour piece is illegal for every base piece; "
            + "the resident on X may still step forward")
    void rule7_enteringXOntoOwnColourIsIllegal() {
        PieceId onX = new PieceId(Colour.RED, 1);
        board.moveTo(onX, new OnTrack(topology.xIndex(Colour.RED)));
        board.assignDirection(onX, Direction.CLOCKWISE);

        List<Move> moves = generator.legalMoves(Colour.RED, 6, board, topology);

        assertEquals(1, moves.size());
        assertEquals(onX, moves.get(0).pieceId());
    }

    @Test
    @DisplayName("nothing on the board and no six rolled produces no legal moves")
    void nothingOnBoardAndNoSixProducesNoMoves() {
        assertTrue(generator.legalMoves(Colour.RED, 3, board, topology).isEmpty());
    }

    @Test
    @DisplayName("Rule 5: a piece jumps over both an opponent piece and a same-colour piece without disturbing them")
    void rule5_jumpsOverOccupiedIntermediateCells() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId opponentBetween = new PieceId(Colour.GREEN, 1);
        PieceId ownBetween = new PieceId(Colour.RED, 2);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(opponentBetween, new OnTrack(11));
        board.moveTo(ownBetween, new OnTrack(13));
        board.assignDirection(ownBetween, Direction.CLOCKWISE);

        List<Move> moves = generator.legalMoves(Colour.RED, 4, board, topology);

        Move move = moves.stream().filter(m -> m.pieceId().equals(mover)).findFirst().orElseThrow();
        assertEquals(new OnTrack(14), move.destination());
        assertFalse(move.capturesSomething());

        move.execute(new MoveContext(board, new EventBus(), new LandingResolver()));

        assertEquals(new OnTrack(14), board.piece(mover).position());
        assertEquals(new OnTrack(11), board.piece(opponentBetween).position());
        assertEquals(new OnTrack(13), board.piece(ownBetween).position());
    }

    @Test
    @DisplayName("a piece already at Home never generates a move")
    void pieceAtHomeNeverGeneratesMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new AtHome());

        List<Move> moves = generator.legalMoves(Colour.RED, 6, board, topology);

        assertTrue(moves.stream().noneMatch(m -> m.pieceId().equals(id)));
    }
}
