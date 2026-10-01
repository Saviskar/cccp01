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
import ludot.events.PieceBlocked;
import ludot.moves.Move;
import ludot.moves.MoveContext;
import ludot.moves.PartialMove;
import ludot.moves.StepMove;
import ludot.random.Coin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveGeneratorTest {

    private final BoardTopology topology = new BoardTopology();
    private final MoveGenerator generator = new MoveGenerator(new MovementCalculator());
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    private List<Move> legalMovesOnly(Colour colour, int roll) {
        return generator.legalMoves(colour, roll, board, topology).legalMoves();
    }

    @Test
    @DisplayName("Rule 2/3: with everything in base, only a six produces a legal move")
    void rule2_onlyASixLeavesBase() {
        assertTrue(legalMovesOnly(Colour.RED, 5).isEmpty());
        assertEquals(4, legalMovesOnly(Colour.RED, 6).size());
    }

    @Test
    @DisplayName("a piece already on the track produces one step move per roll")
    void onTrackPieceProducesOneStepMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        assertEquals(1, moves.size());
        Move move = moves.get(0);
        assertEquals(id, move.pieceId());
        assertEquals(new OnTrack(14), move.destination());
    }

    @Test
    @DisplayName("A-14: landing on an own-colour standard-track cell is legal and forms a block")
    void a14_ownColourTrackLandingFormsBlock() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId resident = new PieceId(Colour.RED, 2);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(resident, new OnTrack(14));
        board.assignDirection(resident, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        Move move = moves.stream().filter(m -> m.pieceId().equals(mover)).findFirst().orElseThrow();
        assertEquals(new OnTrack(14), move.destination());
        assertTrue(move.formsBlock());
        assertFalse(move.capturesSomething());
    }

    @Test
    @DisplayName("A-10: own-colour sharing in the home straight is legal and never forms a block")
    void a10_homeStraightSharingNeverFormsBlock() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId sibling = new PieceId(Colour.RED, 2);
        board.moveTo(mover, new InHomeStraight(1));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(sibling, new InHomeStraight(2));
        board.assignDirection(sibling, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        Move move = moves.stream().filter(m -> m.pieceId().equals(mover)).findFirst().orElseThrow();
        assertEquals(new InHomeStraight(2), move.destination());
        assertFalse(move.formsBlock());
    }

    @Test
    @DisplayName("Rule 10: an overshooting move is not generated")
    void rule10_overshootingMoveIsNotGenerated() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new InHomeStraight(3));
        board.assignDirection(id, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 3);

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

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        assertEquals(1, moves.size());
        assertTrue(moves.get(0).capturesSomething());
    }

    @Test
    @DisplayName("entering base to X onto a single opponent flags capturesSomething")
    void enteringXOntoSingleOpponentFlagsCapture() {
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.xIndex(Colour.RED)));

        List<Move> moves = legalMovesOnly(Colour.RED, 6);

        assertEquals(4, moves.size());
        assertTrue(moves.stream().allMatch(Move::capturesSomething));
    }

    @Test
    @DisplayName("A-25: entering base to X onto an own-colour piece is legal and forms a block; "
            + "the resident on X may still step forward")
    void a25_enteringXOntoOwnColourFormsBlock() {
        PieceId onX = new PieceId(Colour.RED, 1);
        board.moveTo(onX, new OnTrack(topology.xIndex(Colour.RED)));
        board.assignDirection(onX, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 6);

        assertEquals(4, moves.size());
        List<Move> baseEntries = moves.stream().filter(m -> !m.pieceId().equals(onX)).toList();
        assertEquals(3, baseEntries.size());
        assertTrue(baseEntries.stream().allMatch(Move::formsBlock));
        assertTrue(baseEntries.stream().noneMatch(Move::capturesSomething));
    }

    @Test
    @DisplayName("A-25: an opponent block on X makes base entry illegal")
    void a25_enteringXOntoOpponentBlockIsIllegal() {
        int redX = topology.xIndex(Colour.RED);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(redX));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(redX));

        MoveGenerationResult result = generator.legalMoves(Colour.RED, 6, board, topology);

        assertTrue(result.legalMoves().isEmpty());
        assertTrue(result.deadEndObstructions().isEmpty());
    }

    @Test
    @DisplayName("nothing on the board and no six rolled produces no legal moves")
    void nothingOnBoardAndNoSixProducesNoMoves() {
        assertTrue(legalMovesOnly(Colour.RED, 3).isEmpty());
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

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        Move move = moves.stream().filter(m -> m.pieceId().equals(mover)).findFirst().orElseThrow();
        assertEquals(new OnTrack(14), move.destination());
        assertFalse(move.capturesSomething());

        Coin unusedCoin = () -> Direction.CLOCKWISE;
        move.execute(new MoveContext(board, new EventBus(), new LandingResolver(), unusedCoin));

        assertEquals(new OnTrack(14), board.piece(mover).position());
        assertEquals(new OnTrack(11), board.piece(opponentBetween).position());
        assertEquals(new OnTrack(13), board.piece(ownBetween).position());
    }

    @Test
    @DisplayName("a piece already at Home never generates a move")
    void pieceAtHomeNeverGeneratesMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new AtHome());

        List<Move> moves = legalMovesOnly(Colour.RED, 6);

        assertTrue(moves.stream().noneMatch(m -> m.pieceId().equals(id)));
    }

    @Test
    @DisplayName("A-08: a counterclockwise piece's first Approach crossing generates a crossing step move")
    void a08_generatesCrossingStepMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(id, new OnTrack(approach));
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        StepMove move = (StepMove) moves.stream().filter(m -> m.pieceId().equals(id)).findFirst().orElseThrow();
        assertEquals(new OnTrack(topology.step(approach, Direction.COUNTERCLOCKWISE)), move.destination());
        assertTrue(move.crossesApproachWithoutEntering());
    }

    @Test
    @DisplayName("A-08: a counterclockwise piece's second Approach crossing generates a home-straight entry")
    void a08_generatesHomeStraightEntryOnSecondCrossing() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(id, new OnTrack(approach));
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        board.recordApproachCrossing(id);

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        StepMove move = (StepMove) moves.stream().filter(m -> m.pieceId().equals(id)).findFirst().orElseThrow();
        assertEquals(new InHomeStraight(0), move.destination());
        assertFalse(move.crossesApproachWithoutEntering());
    }

    @Test
    @DisplayName("A-16: an obstructed piece with no other legal full move gets a partial move")
    void a16_obstructedPieceWithNoAlternativeGetsPartialMove() {
        PieceId mover = new PieceId(Colour.GREEN, 1);
        PieceId blocker1 = new PieceId(Colour.RED, 1);
        PieceId blocker2 = new PieceId(Colour.RED, 2);
        board.moveTo(new PieceId(Colour.GREEN, 2), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 3), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 4), new AtHome());
        board.moveTo(mover, new OnTrack(0));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(blocker1, new OnTrack(4));
        board.moveTo(blocker2, new OnTrack(4));

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 6, board, topology);

        assertEquals(1, result.legalMoves().size());
        assertTrue(result.deadEndObstructions().isEmpty());
        PartialMove move = assertInstanceOf(PartialMove.class, result.legalMoves().get(0));
        assertEquals(mover, move.pieceId());
        assertEquals(new OnTrack(3), move.destination());
        assertEquals(3, move.cellsMoved());
        assertEquals(new OnTrack(6), move.intendedDestination());
        assertEquals(blocker1, move.blockingPieceId());
        assertFalse(move.capturesSomething());
        assertFalse(move.formsBlock());
    }

    @Test
    @DisplayName("A-14/A-16: a partial move landing on an own-colour piece forms a block")
    void a14_partialMoveLandingOnOwnColourFormsBlock() {
        PieceId mover = new PieceId(Colour.GREEN, 1);
        PieceId resident = new PieceId(Colour.GREEN, 4);
        board.moveTo(new PieceId(Colour.GREEN, 2), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 3), new AtHome());
        board.moveTo(mover, new OnTrack(0));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(resident, new OnTrack(3));
        board.assignDirection(resident, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.RED, 1), new OnTrack(4));
        board.moveTo(new PieceId(Colour.RED, 2), new OnTrack(4));

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 6, board, topology);

        assertEquals(1, result.legalMoves().size());
        PartialMove move = assertInstanceOf(PartialMove.class, result.legalMoves().get(0));
        assertEquals(mover, move.pieceId());
        assertEquals(new OnTrack(3), move.destination());
        assertTrue(move.formsBlock());
        assertFalse(move.capturesSomething());
    }

    @Test
    @DisplayName("A-16: an obstructed piece with another legal full move elsewhere gets no move at all")
    void a16_obstructedPieceWithAlternativeGetsNoMove() {
        PieceId obstructed = new PieceId(Colour.GREEN, 1);
        PieceId free = new PieceId(Colour.GREEN, 2);
        board.moveTo(new PieceId(Colour.GREEN, 3), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 4), new AtHome());
        board.moveTo(obstructed, new OnTrack(0));
        board.assignDirection(obstructed, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.RED, 1), new OnTrack(4));
        board.moveTo(new PieceId(Colour.RED, 2), new OnTrack(4));
        board.moveTo(free, new OnTrack(20));
        board.assignDirection(free, Direction.CLOCKWISE);

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 6, board, topology);

        assertEquals(1, result.legalMoves().size());
        assertEquals(free, result.legalMoves().get(0).pieceId());
        assertTrue(result.deadEndObstructions().isEmpty());
    }

    @Test
    @DisplayName("A-48: an adjacent block with no alternative produces a dead-end fact and no move")
    void a48_adjacentBlockWithNoAlternativeProducesDeadEndFact() {
        PieceId mover = new PieceId(Colour.GREEN, 1);
        PieceId blocker1 = new PieceId(Colour.RED, 1);
        PieceId blocker2 = new PieceId(Colour.RED, 2);
        board.moveTo(new PieceId(Colour.GREEN, 2), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 3), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 4), new AtHome());
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(blocker1, new OnTrack(11));
        board.moveTo(blocker2, new OnTrack(11));

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 4, board, topology);

        assertTrue(result.legalMoves().isEmpty());
        assertEquals(1, result.deadEndObstructions().size());
        PieceBlocked blocked = result.deadEndObstructions().get(0);
        assertEquals(mover, blocked.pieceId());
        assertEquals(new OnTrack(10), blocked.from());
        assertEquals(new OnTrack(14), blocked.intendedDestination());
        assertEquals(blocker1, blocked.blockingPieceId());
    }

    @Test
    @DisplayName("A-48: a mixed dead-end and partial-move result surfaces both in the generator's output")
    void a48_mixedDeadEndAndPartialBothSurfaceInGeneratorResult() {
        PieceId deadEndMover = new PieceId(Colour.GREEN, 1);
        PieceId partialMover = new PieceId(Colour.GREEN, 2);
        board.moveTo(new PieceId(Colour.GREEN, 3), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 4), new AtHome());
        board.moveTo(deadEndMover, new OnTrack(20));
        board.assignDirection(deadEndMover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.RED, 1), new OnTrack(21));
        board.moveTo(new PieceId(Colour.RED, 2), new OnTrack(21));
        board.moveTo(partialMover, new OnTrack(0));
        board.assignDirection(partialMover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.RED, 3), new OnTrack(4));
        board.moveTo(new PieceId(Colour.RED, 4), new OnTrack(4));

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 6, board, topology);

        assertEquals(1, result.legalMoves().size());
        assertEquals(partialMover, result.legalMoves().get(0).pieceId());
        assertEquals(1, result.deadEndObstructions().size());
        assertEquals(deadEndMover, result.deadEndObstructions().get(0).pieceId());
    }

    @Test
    @DisplayName("a partial move landing on a single opponent still captures")
    void partialMoveLandingOnSingleOpponentCaptures() {
        PieceId mover = new PieceId(Colour.GREEN, 1);
        board.moveTo(new PieceId(Colour.GREEN, 2), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 3), new AtHome());
        board.moveTo(new PieceId(Colour.GREEN, 4), new AtHome());
        board.moveTo(mover, new OnTrack(0));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.RED, 1), new OnTrack(4));
        board.moveTo(new PieceId(Colour.RED, 2), new OnTrack(4));
        board.moveTo(new PieceId(Colour.BLUE, 1), new OnTrack(3));

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 6, board, topology);

        assertEquals(1, result.legalMoves().size());
        PartialMove move = assertInstanceOf(PartialMove.class, result.legalMoves().get(0));
        assertTrue(move.capturesSomething());
    }
}
