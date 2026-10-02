package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.Energised;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Sick;
import ludot.events.EventBus;
import ludot.events.PieceBlocked;
import ludot.moves.BlockMove;
import ludot.moves.Move;
import ludot.moves.MoveContext;
import ludot.moves.MoveResult;
import ludot.moves.MysteryHandler;
import ludot.moves.PartialMove;
import ludot.moves.StepMove;
import ludot.random.Coin;
import ludot.random.RandomPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveGeneratorTest {

    @Mock
    private RandomPicker picker;

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

    // A-28: spawns the mystery cell at exactly `index`, assuming a piece is already on the track.
    private void placeMysteryCellAt(int index) {
        when(picker.pick(anyList())).thenReturn(index);
        board.tickMysteryCell(picker);
        board.tickMysteryCell(picker);
        board.tickMysteryCell(picker);
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
        assertEquals(List.of(id), move.pieceIds());
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

        Move move = moves.stream().filter(m -> m.pieceIds().contains(mover)).findFirst().orElseThrow();
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

        Move move = moves.stream().filter(m -> m.pieceIds().contains(mover)).findFirst().orElseThrow();
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
        List<Move> baseEntries = moves.stream().filter(m -> !m.pieceIds().contains(onX)).toList();
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

        Move move = moves.stream().filter(m -> m.pieceIds().contains(mover)).findFirst().orElseThrow();
        assertEquals(new OnTrack(14), move.destination());
        assertFalse(move.capturesSomething());

        Coin unusedCoin = () -> Direction.CLOCKWISE;
        MysteryHandler unusedMysteryHandler = (pieceId, b, e) -> false;
        move.execute(new MoveContext(board, new EventBus(), new LandingResolver(), unusedCoin, unusedMysteryHandler));

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

        assertTrue(moves.stream().noneMatch(m -> m.pieceIds().contains(id)));
    }

    @Test
    @DisplayName("A-08: a counterclockwise piece's first Approach crossing generates a crossing step move")
    void a08_generatesCrossingStepMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(id, new OnTrack(approach));
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        StepMove move = (StepMove) moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow();
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
        board.recordCapture(id); // A-07: isolates A-08's crossing behaviour from the capture gate

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        StepMove move = (StepMove) moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow();
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
        assertEquals(List.of(free), result.legalMoves().get(0).pieceIds());
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
        assertEquals(List.of(partialMover), result.legalMoves().get(0).pieceIds());
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

    private Optional<BlockMove> onlyBlockMove(List<Move> moves) {
        return moves.stream().filter(m -> m instanceof BlockMove).map(m -> (BlockMove) m).findFirst();
    }

    @Test
    @DisplayName("T-4/A-17: a block moves floor(roll / block size) cells")
    void t4_a17_blockMoveUsesFloorDivisionOfRollByBlockSize() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 5);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(List.of(member1, member2), move.pieceIds());
        assertEquals(new OnTrack(10), move.origin());
        assertEquals(new OnTrack(12), move.destination());
        assertEquals(2, move.cellsPerPiece());
    }

    @Test
    @DisplayName("A-17: a zero-cell block move (floor(roll / size) == 0) is not generated")
    void a17_blockMoveIllegalWhenFloorDivisionIsZero() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        PieceId member3 = new PieceId(Colour.RED, 3);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(member3, new OnTrack(10));
        board.assignDirection(member3, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 2);

        assertTrue(onlyBlockMove(moves).isEmpty());
    }

    @Test
    @DisplayName("A-17: the block moves in the members' shared direction when they agree")
    void a17_blockDirectionSharedWhenMembersAgree() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 5);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(Direction.CLOCKWISE, move.direction());
    }

    @Test
    @DisplayName("A-17: when members disagree, the block moves in the farthest-from-home member's direction")
    void a17_blockDirectionFarthestFromHomeWhenMembersDisagree() {
        PieceId clockwiseMember = new PieceId(Colour.RED, 1);
        PieceId counterclockwiseMember = new PieceId(Colour.RED, 2);
        board.moveTo(clockwiseMember, new OnTrack(20));
        board.assignDirection(clockwiseMember, Direction.CLOCKWISE);
        board.moveTo(counterclockwiseMember, new OnTrack(20));
        board.assignDirection(counterclockwiseMember, Direction.COUNTERCLOCKWISE);
        board.recordApproachCrossing(counterclockwiseMember); // past its first crossing (A-08)

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        // Clockwise distance from 20: 10; counterclockwise distance from 20 (past first crossing): 54.
        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(Direction.COUNTERCLOCKWISE, move.direction());
        assertEquals(new OnTrack(18), move.destination());
    }

    @Test
    @DisplayName("A-17: a tied distance between differing directions breaks to clockwise")
    void a17_blockDirectionTiesBreakToClockwise() {
        PieceId clockwiseMember = new PieceId(Colour.RED, 1);
        PieceId counterclockwiseMember = new PieceId(Colour.RED, 2);
        board.moveTo(clockwiseMember, new OnTrack(50));
        board.assignDirection(clockwiseMember, Direction.CLOCKWISE);
        board.moveTo(counterclockwiseMember, new OnTrack(50));
        board.assignDirection(counterclockwiseMember, Direction.COUNTERCLOCKWISE);
        board.recordApproachCrossing(counterclockwiseMember); // past its first crossing (A-08)

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        // Both directions are exactly 32 cells from home (26 to Approach + 6); the tie goes to clockwise.
        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(Direction.CLOCKWISE, move.direction());
        assertEquals(new OnTrack(0), move.destination());
    }

    @Test
    @DisplayName("A-18: a block move never enters the home straight, even when it passes the Approach cell")
    void a18_blockMoveNeverEntersHomeStraight() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(22));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(22));
        board.assignDirection(member2, Direction.CLOCKWISE);

        // Red's Approach is 24; floor(8/2)=4 cells from 22 walks 22->23->24->25->26, past Approach.
        List<Move> moves = legalMovesOnly(Colour.RED, 8);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(new OnTrack(26), move.destination());
    }

    @Test
    @DisplayName("A-19: a block move landing on a single opponent flags capturesSomething")
    void a19_blockMoveCapturesSingleOpponentAndMarksCapturesSomething() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(12));

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertTrue(move.capturesSomething());
    }

    @Test
    @DisplayName("A-50: a block move is illegal when an opponent block occupies a cell on its path")
    void a50_blockMoveIllegalWhenOpponentBlockOnPath() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(12));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(12));

        // floor(6/2)=3 cells from 10 walks 10->11->12->13, passing the opponent block at 12.
        MoveGenerationResult result = generator.legalMoves(Colour.RED, 6, board, topology);

        assertTrue(onlyBlockMove(result.legalMoves()).isEmpty());
    }

    @Test
    @DisplayName("T-8/A-20: a block move captures a same-size opponent block on the landing cell")
    void t8_a20_blockMoveCapturesSameSizeOpponentBlockOnLandingCell() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(12));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(12));

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(new OnTrack(12), move.destination());
        assertTrue(move.capturesSomething());
    }

    @Test
    @DisplayName("T-8/A-20: a block move is illegal when a different-size opponent block occupies the landing cell")
    void t8_a20_blockMoveIllegalWhenDifferentSizeOpponentBlockOnLandingCell() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(12));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(12));
        board.moveTo(new PieceId(Colour.GREEN, 3), new OnTrack(12));

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        assertTrue(onlyBlockMove(moves).isEmpty());
    }

    @Test
    @DisplayName("T-8/A-20: executing a block-vs-block capture returns every opponent member to base, "
            + "increments each capturing member's count once, and reports a single capture for the bonus roll")
    void t8_a20_blockVsBlockCaptureExecutesEndToEnd() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        PieceId opponent1 = new PieceId(Colour.GREEN, 1);
        PieceId opponent2 = new PieceId(Colour.GREEN, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(opponent1, new OnTrack(12));
        board.moveTo(opponent2, new OnTrack(12));

        List<Move> moves = legalMovesOnly(Colour.RED, 4);
        BlockMove move = onlyBlockMove(moves).orElseThrow();

        Coin unusedCoin = () -> Direction.CLOCKWISE;
        MysteryHandler unusedMysteryHandler = (pieceId, b, e) -> false;
        MoveResult result = move.execute(new MoveContext(board, new EventBus(), new LandingResolver(), unusedCoin, unusedMysteryHandler));

        assertInstanceOf(InBase.class, board.piece(opponent1).position());
        assertInstanceOf(InBase.class, board.piece(opponent2).position());
        assertEquals(1, board.piece(member1).captureCount());
        assertEquals(1, board.piece(member2).captureCount());
        assertTrue(result.captured()); // A-53: a single flag, regardless of how many pieces were captured
    }

    @Test
    @DisplayName("T-8/A-20: a block-vs-block capture generalises past size 2 — a 3-member block "
            + "captures a same-size 3-member block")
    void t8_a20_blockVsBlockCaptureExecutesEndToEndForThreeMemberBlocks() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        PieceId member3 = new PieceId(Colour.RED, 3);
        PieceId opponent1 = new PieceId(Colour.GREEN, 1);
        PieceId opponent2 = new PieceId(Colour.GREEN, 2);
        PieceId opponent3 = new PieceId(Colour.GREEN, 3);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(member3, new OnTrack(10));
        board.assignDirection(member3, Direction.CLOCKWISE);
        board.moveTo(opponent1, new OnTrack(12));
        board.moveTo(opponent2, new OnTrack(12));
        board.moveTo(opponent3, new OnTrack(12));

        // floor(6/3)=2 cells from 10 lands exactly on the opponent block at 12.
        List<Move> moves = legalMovesOnly(Colour.RED, 6);
        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertTrue(move.capturesSomething());

        Coin unusedCoin = () -> Direction.CLOCKWISE;
        MysteryHandler unusedMysteryHandler = (pieceId, b, e) -> false;
        MoveResult result = move.execute(new MoveContext(board, new EventBus(), new LandingResolver(), unusedCoin, unusedMysteryHandler));

        assertInstanceOf(InBase.class, board.piece(opponent1).position());
        assertInstanceOf(InBase.class, board.piece(opponent2).position());
        assertInstanceOf(InBase.class, board.piece(opponent3).position());
        assertEquals(1, board.piece(member1).captureCount());
        assertEquals(1, board.piece(member2).captureCount());
        assertEquals(1, board.piece(member3).captureCount());
        assertTrue(result.captured()); // A-53: a single flag, regardless of how many pieces were captured
    }

    @Test
    @DisplayName("A-50: a block move passes single pieces of either colour and own-colour blocks")
    void a50_blockMoveLegalPastSinglePiecesAndOwnBlocks() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        PieceId ownBlockMember1 = new PieceId(Colour.RED, 3);
        PieceId ownBlockMember2 = new PieceId(Colour.RED, 4);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(11));
        board.moveTo(ownBlockMember1, new OnTrack(12));
        board.assignDirection(ownBlockMember1, Direction.CLOCKWISE);
        board.moveTo(ownBlockMember2, new OnTrack(12));
        board.assignDirection(ownBlockMember2, Direction.CLOCKWISE);

        // floor(6/2)=3 cells from 10 walks 10->11->12->13, passing the single opponent at 11
        // and the own-colour block at 12.
        List<Move> moves = legalMovesOnly(Colour.RED, 6);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(new OnTrack(13), move.destination());
        assertFalse(move.capturesSomething());
    }

    @Test
    @DisplayName("A-08: a counterclockwise block move past Approach records a crossing for every member")
    void a08_counterclockwiseBlockMovePastApproachRecordsCrossingForEveryMember() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(26));
        board.assignDirection(member1, Direction.COUNTERCLOCKWISE);
        board.moveTo(member2, new OnTrack(26));
        board.assignDirection(member2, Direction.COUNTERCLOCKWISE);

        // floor(6/2)=3 cells counterclockwise from 26 walks 26->25->24->23, leaving Approach (24).
        List<Move> moves = legalMovesOnly(Colour.RED, 6);
        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(new OnTrack(23), move.destination());
        assertTrue(move.crossesApproachWithoutEntering());

        Coin unusedCoin = () -> Direction.CLOCKWISE;
        MysteryHandler unusedMysteryHandler = (pieceId, b, e) -> false;
        move.execute(new MoveContext(board, new EventBus(), new LandingResolver(), unusedCoin, unusedMysteryHandler));

        assertEquals(1, board.piece(member1).ccwApproachCrossings());
        assertEquals(1, board.piece(member2).ccwApproachCrossings());
    }

    @Test
    @DisplayName("A-08: a counterclockwise block move landing exactly on Approach records no crossing")
    void a08_counterclockwiseBlockLandingExactlyOnApproachRecordsNoCrossing() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(26));
        board.assignDirection(member1, Direction.COUNTERCLOCKWISE);
        board.moveTo(member2, new OnTrack(26));
        board.assignDirection(member2, Direction.COUNTERCLOCKWISE);

        // floor(4/2)=2 cells counterclockwise from 26 walks 26->25->24, landing exactly on Approach.
        List<Move> moves = legalMovesOnly(Colour.RED, 4);
        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(new OnTrack(24), move.destination());
        assertFalse(move.crossesApproachWithoutEntering());

        Coin unusedCoin = () -> Direction.CLOCKWISE;
        MysteryHandler unusedMysteryHandler = (pieceId, b, e) -> false;
        move.execute(new MoveContext(board, new EventBus(), new LandingResolver(), unusedCoin, unusedMysteryHandler));

        assertEquals(0, board.piece(member1).ccwApproachCrossings());
        assertEquals(0, board.piece(member2).ccwApproachCrossings());
    }

    @Test
    @DisplayName("breaksBlock is true for an individual move generated for a piece currently in a block")
    void breaksBlockTrueWhenPieceLeavesOwnBlock() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 3);

        List<StepMove> stepMoves = moves.stream().filter(m -> m instanceof StepMove).map(m -> (StepMove) m).toList();
        assertEquals(2, stepMoves.size());
        assertTrue(stepMoves.stream().allMatch(StepMove::breaksBlock));
    }

    @Test
    @DisplayName("breaksBlock is false for an individual move generated for a piece not currently in a block")
    void breaksBlockFalseOtherwise() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 3);

        StepMove move = assertInstanceOf(StepMove.class, moves.get(0));
        assertFalse(move.breaksBlock());
    }

    @Test
    @DisplayName("T-5/A-21: breaking off from a block uses the piece's own original direction")
    void t5_a21_breakingOffFromBlockUsesPiecesOriginalDirection() {
        PieceId clockwiseMember = new PieceId(Colour.RED, 1);
        PieceId counterclockwiseMember = new PieceId(Colour.RED, 2);
        board.moveTo(clockwiseMember, new OnTrack(10));
        board.assignDirection(clockwiseMember, Direction.CLOCKWISE);
        board.moveTo(counterclockwiseMember, new OnTrack(10));
        board.assignDirection(counterclockwiseMember, Direction.COUNTERCLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 3);

        StepMove clockwiseMove = moves.stream()
                .filter(m -> m instanceof StepMove && m.pieceIds().contains(clockwiseMember))
                .map(m -> (StepMove) m).findFirst().orElseThrow();
        assertEquals(Direction.CLOCKWISE, clockwiseMove.direction());
        assertEquals(new OnTrack(13), clockwiseMove.destination());

        StepMove counterclockwiseMove = moves.stream()
                .filter(m -> m instanceof StepMove && m.pieceIds().contains(counterclockwiseMember))
                .map(m -> (StepMove) m).findFirst().orElseThrow();
        assertEquals(Direction.COUNTERCLOCKWISE, counterclockwiseMove.direction());
        assertEquals(new OnTrack(7), counterclockwiseMove.destination());
    }

    @Test
    @DisplayName("T-5/A-21: breaksBlock is true on a partial move generated from a block origin")
    void t5_a21_breaksBlockTrueOnPartialMoveFromBlockOrigin() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(new PieceId(Colour.RED, 3), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 4), new AtHome());
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.BLUE, 1), new OnTrack(12));
        board.moveTo(new PieceId(Colour.BLUE, 2), new OnTrack(12));

        // floor(6/2)=3 cells from 10 walks 10->11->12->13, so the block move is obstructed by A-50
        // and never generated. Each individual 6-cell walk is also obstructed at 12, stopping at 11
        // (cellsWalked=1), so both members fall back to a partial move.
        List<Move> moves = legalMovesOnly(Colour.RED, 6);

        assertEquals(2, moves.size());
        assertTrue(moves.stream().allMatch(m -> m instanceof PartialMove));
        PartialMove move1 = (PartialMove) moves.stream().filter(m -> m.pieceIds().contains(member1)).findFirst().orElseThrow();
        PartialMove move2 = (PartialMove) moves.stream().filter(m -> m.pieceIds().contains(member2)).findFirst().orElseThrow();
        assertEquals(new OnTrack(11), move1.destination());
        assertEquals(1, move1.cellsMoved());
        assertTrue(move1.breaksBlock());
        assertTrue(move2.breaksBlock());
    }

    @Test
    @DisplayName("T-6/A-22: forcedMove builds a plain step move when the path is clear")
    void t6_a22_forcedMoveReachable_buildsStepMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);

        ForcedMoveOutcome outcome =
                generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, true, board, topology);

        ForcedMoveOutcome.Movable movable = assertInstanceOf(ForcedMoveOutcome.Movable.class, outcome);
        StepMove move = assertInstanceOf(StepMove.class, movable.move());
        assertEquals(new OnTrack(16), move.destination());
        assertTrue(move.breaksBlock());
    }

    @Test
    @DisplayName("A-55: forcedMove ignores a member's Alpha effect, moving exactly its plain share")
    void t6_a55_forcedMoveIgnoresPieceEffect() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Energised());

        ForcedMoveOutcome outcome =
                generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, true, board, topology);

        ForcedMoveOutcome.Movable movable = assertInstanceOf(ForcedMoveOutcome.Movable.class, outcome);
        StepMove move = assertInstanceOf(StepMove.class, movable.move());
        assertEquals(new OnTrack(16), move.destination()); // 10 + 6 units, not doubled to 12
    }

    @Test
    @DisplayName("A-52: forcedMove builds a partial move when obstructed with cells to spare")
    void a52_forcedMoveObstructedWithRoom_buildsPartialMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(4));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(4));

        ForcedMoveOutcome outcome =
                generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, true, board, topology);

        ForcedMoveOutcome.Movable movable = assertInstanceOf(ForcedMoveOutcome.Movable.class, outcome);
        PartialMove move = assertInstanceOf(PartialMove.class, movable.move());
        assertEquals(new OnTrack(3), move.destination());
        assertEquals(3, move.cellsMoved());
    }

    @Test
    @DisplayName("A-52: forcedMove returns a dead end when the obstructing block is immediately adjacent")
    void a52_forcedMoveObstructedAdjacent_returnsDeadEnd() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(11));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(11));

        ForcedMoveOutcome outcome =
                generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, true, board, topology);

        ForcedMoveOutcome.DeadEnd deadEnd = assertInstanceOf(ForcedMoveOutcome.DeadEnd.class, outcome);
        assertEquals(id, deadEnd.blocked().pieceId());
        assertEquals(new OnTrack(10), deadEnd.blocked().from());
        assertEquals(new OnTrack(16), deadEnd.blocked().intendedDestination());
    }

    @Test
    @DisplayName("T-6: forcedMove's overshoot branch is defensive only — unreachable via real play "
            + "(A-05/A-09: minimum on-track distance to home is 6, matching A-22's maximum unit share)")
    void t6_forcedMoveThrowsOnOvershoot_defensiveOnlyUnreachableInPlay() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new InHomeStraight(3));
        board.assignDirection(id, Direction.CLOCKWISE);

        assertThrows(IllegalStateException.class,
                () -> generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, false, board, topology));
    }

    @Test
    @DisplayName("T-6/A-22: forcedMove lands a piece exactly on Home when it sits on the Approach cell")
    void t6_a22_forcedMoveFromApproach_landsExactlyOnHome() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(id, new OnTrack(approach));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.recordCapture(id); // stays valid once T-7/phase 4f gates home-straight entry on a capture

        ForcedMoveOutcome outcome =
                generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, true, board, topology);

        ForcedMoveOutcome.Movable movable = assertInstanceOf(ForcedMoveOutcome.Movable.class, outcome);
        StepMove move = assertInstanceOf(StepMove.class, movable.move());
        assertEquals(new AtHome(), move.destination());
    }

    @Test
    @DisplayName("A-07: a piece with no captures generates a step that continues past the Approach")
    void a07_ineligiblePieceGeneratesAStepThatContinuesPastApproach() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(id, new OnTrack(approach));
        board.assignDirection(id, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        StepMove move = assertInstanceOf(
                StepMove.class, moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow());
        assertEquals(new OnTrack(topology.step(approach, Direction.CLOCKWISE)), move.destination());
    }

    @Test
    @DisplayName("A-07: a piece becomes eligible for the home straight once it has captured")
    void a07_eligiblePieceAfterCaptureEntersHomeStraight() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(id, new OnTrack(approach));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.recordCapture(id);

        List<Move> moves = legalMovesOnly(Colour.RED, 1);

        StepMove move = assertInstanceOf(
                StepMove.class, moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow());
        assertEquals(new InHomeStraight(0), move.destination());
    }

    @Test
    @DisplayName("A-29: a step move landing exactly on the mystery cell has landsOnMystery true")
    void a29_stepMoveLandingOnMysteryCellIsTrue() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        placeMysteryCellAt(14);

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        StepMove move = assertInstanceOf(
                StepMove.class, moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow());
        assertEquals(new OnTrack(14), move.destination());
        assertTrue(move.landsOnMystery());
    }

    @Test
    @DisplayName("A-29: a step move landing elsewhere has landsOnMystery false")
    void a29_stepMoveNotLandingOnMysteryCellIsFalse() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        placeMysteryCellAt(20); // not the destination (14)

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        StepMove move = assertInstanceOf(
                StepMove.class, moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow());
        assertFalse(move.landsOnMystery());
    }

    @Test
    @DisplayName("A-29: a step move passing over the mystery cell without landing on it does not trigger it")
    void a29_stepMovePassingOverMysteryCellDoesNotTrigger() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        placeMysteryCellAt(12); // on the 10->14 path, but not the landing cell

        List<Move> moves = legalMovesOnly(Colour.RED, 4);

        StepMove move = assertInstanceOf(
                StepMove.class, moves.stream().filter(m -> m.pieceIds().contains(id)).findFirst().orElseThrow());
        assertFalse(move.landsOnMystery());
    }

    @Test
    @DisplayName("A-29: an obstructed partial move landing exactly on the mystery cell has landsOnMystery true")
    void a29_partialMoveLandingOnMysteryCellIsTrue() {
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
        placeMysteryCellAt(3); // the partial move's actual landing cell

        MoveGenerationResult result = generator.legalMoves(Colour.GREEN, 6, board, topology);

        PartialMove move = assertInstanceOf(PartialMove.class, result.legalMoves().get(0));
        assertEquals(new OnTrack(3), move.destination());
        assertTrue(move.landsOnMystery());
    }

    @Test
    @DisplayName("A-29: entering from base never lands on the mystery cell, even when the mystery cell is at X")
    void a29_enterFromBaseNeverLandsOnMysteryEvenAtX() {
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(5)); // so anyPieceOnTrack() starts the A-28 timer
        placeMysteryCellAt(topology.xIndex(Colour.RED));

        List<Move> moves = legalMovesOnly(Colour.RED, 6);

        assertTrue(moves.stream().noneMatch(Move::landsOnMystery));
    }

    @Test
    @DisplayName("A-54: a T-6 forced-break member landing exactly on the mystery cell has landsOnMystery true")
    void a54_forcedMoveLandingOnMysteryCellIsTrue() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(15));
        board.assignDirection(id, Direction.CLOCKWISE);
        placeMysteryCellAt(21); // 15 + 6 units clockwise

        ForcedMoveOutcome outcome =
                generator.forcedMove(board.piece(id), 6, Direction.CLOCKWISE, false, board, topology);

        ForcedMoveOutcome.Movable movable = assertInstanceOf(ForcedMoveOutcome.Movable.class, outcome);
        StepMove move = assertInstanceOf(StepMove.class, movable.move());
        assertEquals(new OnTrack(21), move.destination());
        assertTrue(move.landsOnMystery());
    }

    @Test
    @DisplayName("T-12/A-32: an energised piece moves double the roll")
    void t12_a32_energisedPieceMovesDoubleTheRoll() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Energised());

        List<Move> moves = legalMovesOnly(Colour.RED, 3);

        StepMove move = assertInstanceOf(StepMove.class, moves.get(0));
        assertEquals(new OnTrack(16), move.destination()); // 10 + (3 * 2)
        assertEquals(6, move.units());
    }

    @Test
    @DisplayName("T-12/A-32: a sick piece moves floor(roll / 2)")
    void t12_a32_sickPieceMovesHalfTheRollFloored() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Sick());

        List<Move> moves = legalMovesOnly(Colour.RED, 5);

        StepMove move = assertInstanceOf(StepMove.class, moves.get(0));
        assertEquals(new OnTrack(12), move.destination()); // 10 + floor(5 / 2)
        assertEquals(2, move.units());
    }

    @Test
    @DisplayName("T-12/A-32: a sick piece whose effective roll is 0 cannot move")
    void t12_a32_sickPieceWithEffectiveZeroCannotMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Sick());

        MoveGenerationResult result = generator.legalMoves(Colour.RED, 1, board, topology);

        assertTrue(result.legalMoves().isEmpty());
        assertTrue(result.deadEndObstructions().isEmpty());
    }

    @Test
    @DisplayName("T-12/A-18: a block move ignores a member's Alpha effect")
    void t12_a18_blockMoveIgnoresPieceEffect() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.applyEffect(member1, new Energised());
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);

        List<Move> moves = legalMovesOnly(Colour.RED, 5);

        BlockMove move = onlyBlockMove(moves).orElseThrow();
        assertEquals(2, move.cellsPerPiece()); // floor(5 / 2), unaffected by member1's Energised effect
    }

    @Test
    @DisplayName("T-12/A-32/A-49: an energised piece's doubled roll can overshoot a raw-legal move")
    void t12_a49_energisedOvershootIsIllegal() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new InHomeStraight(3));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Energised());

        // Raw roll 2 reaches Home exactly (cell 3 -> cell 4 -> Home); energised doubles it to 4,
        // which overshoots and makes the move illegal.
        List<Move> moves = legalMovesOnly(Colour.RED, 2);

        assertTrue(moves.isEmpty());
    }
}
