package ludot.players;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.moves.BlockMove;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;
import ludot.moves.StepMove;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A-37/A-65/A-66/A-67: Green's blocking-first behaviour. */
class GreenStrategyTest {

    private final GreenStrategy strategy = new GreenStrategy(new MoveRanking());
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    private static Move nonBreakingStep(PieceId id, int origin, int destination, boolean formsBlock) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                false, formsBlock, false, false, false);
    }

    private static Move breakingStep(PieceId id, int origin, int destination) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                false, false, true, false, false);
    }

    private static Move capturingNonBreakingStep(PieceId id, int origin, int destination) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                true, false, false, false, false);
    }

    @Test
    @DisplayName("A-65: on a six, a move that forms a new block beats bringing a piece out of base")
    void a65_formsNewBlockOverBaseExitOnSix() {
        Move baseExit = new EnterFromBase(new PieceId(Colour.GREEN, 1), new OnTrack(39), false, false);
        Move formsBlock = nonBreakingStep(new PieceId(Colour.GREEN, 2), 20, 26, true);

        assertEquals(formsBlock, strategy.choose(List.of(baseExit, formsBlock), board));
    }

    @Test
    @DisplayName("A-40: ties between new-block-forming moves break by the lowest piece number")
    void a65_tieBreaksNewBlockFormingMovesByLowestPieceNumber() {
        Move baseExit = new EnterFromBase(new PieceId(Colour.GREEN, 4), new OnTrack(39), false, false);
        Move formsBlockG3 = nonBreakingStep(new PieceId(Colour.GREEN, 3), 20, 26, true);
        Move formsBlockG1 = nonBreakingStep(new PieceId(Colour.GREEN, 1), 10, 16, true);

        assertEquals(formsBlockG1, strategy.choose(List.of(baseExit, formsBlockG3, formsBlockG1), board));
    }

    @Test
    @DisplayName("A-65: a base-exit forming a block is grouped with a step move forming a block, not excluded")
    void a65_baseExitFormingBlockIsIncludedWithStepMoveFormingBlock() {
        Move baseExitFormsBlock = new EnterFromBase(new PieceId(Colour.GREEN, 1), new OnTrack(39), false, true); // G3 already on X
        Move stepFormsBlock = nonBreakingStep(new PieceId(Colour.GREEN, 2), 20, 26, true); // lands on G4 at 26

        assertEquals(baseExitFormsBlock, strategy.choose(List.of(stepFormsBlock, baseExitFormsBlock), board));
    }

    @Test
    @DisplayName("A-65: brings a piece out of base when no new-block-forming move exists")
    void a65_bringsPieceFromBaseWhenNoBlockFormingMoveExists() {
        Move baseExit = new EnterFromBase(new PieceId(Colour.GREEN, 1), new OnTrack(39), false, false);
        Move ordinary = nonBreakingStep(new PieceId(Colour.GREEN, 2), 20, 26, false);

        assertEquals(baseExit, strategy.choose(List.of(ordinary, baseExit), board));
    }

    @Test
    @DisplayName("A-40: ties between base-exits break by the lowest piece number")
    void a40_tieBreaksBaseExitsByLowestPieceNumber() {
        Move exitG3 = new EnterFromBase(new PieceId(Colour.GREEN, 3), new OnTrack(39), false, false);
        Move exitG1 = new EnterFromBase(new PieceId(Colour.GREEN, 1), new OnTrack(39), false, false);

        assertEquals(exitG1, strategy.choose(List.of(exitG3, exitG1), board));
    }

    @Test
    @DisplayName("A-65: the base-exit tier is checked before the block-move tier")
    void a65_baseExitTierFiresBeforeBlockMoveTier() {
        PieceId g2 = new PieceId(Colour.GREEN, 2);
        PieceId g3 = new PieceId(Colour.GREEN, 3);

        Move baseExit = new EnterFromBase(new PieceId(Colour.GREEN, 1), new OnTrack(39), false, false);
        Move blockMove = new BlockMove(List.of(g2, g3), new OnTrack(10), new OnTrack(13), 6, 3,
                Direction.CLOCKWISE, false, false);

        assertEquals(baseExit, strategy.choose(List.of(blockMove, baseExit), board));
    }

    @Test
    @DisplayName("A-65: takes a block move over an ordinary non-breaking move when no base-exit is available")
    void a65_takesBlockMoveWhenNoBaseExitAvailable() {
        PieceId g2 = new PieceId(Colour.GREEN, 2);
        PieceId g3 = new PieceId(Colour.GREEN, 3);

        Move blockMove = new BlockMove(List.of(g2, g3), new OnTrack(10), new OnTrack(13), 6, 3,
                Direction.CLOCKWISE, false, false);
        Move ordinary = nonBreakingStep(new PieceId(Colour.GREEN, 1), 30, 36, false);

        assertEquals(blockMove, strategy.choose(List.of(ordinary, blockMove), board));
    }

    @Test
    @DisplayName("A-40: ties between two legal block moves break by the lowest member number")
    void a65_tieBreaksTwoBlockMovesByLowestMemberNumber() {
        PieceId g3 = new PieceId(Colour.GREEN, 3);
        PieceId g4 = new PieceId(Colour.GREEN, 4);
        PieceId g1 = new PieceId(Colour.GREEN, 1);
        PieceId g2 = new PieceId(Colour.GREEN, 2);

        Move blockHighNumbers = new BlockMove(List.of(g3, g4), new OnTrack(10), new OnTrack(13), 6, 3,
                Direction.CLOCKWISE, false, false);
        Move blockLowNumbers = new BlockMove(List.of(g1, g2), new OnTrack(20), new OnTrack(23), 6, 3,
                Direction.CLOCKWISE, false, false);

        assertEquals(blockLowNumbers, strategy.choose(List.of(blockHighNumbers, blockLowNumbers), board));
    }

    @Test
    @DisplayName("A-37/A-66: prefers a non-breaking move over a block-breaking move, even if farther from home")
    void a37_a66_prefersNonBreakingMoveOverBreakingMove() {
        PieceId breaking = new PieceId(Colour.GREEN, 1); // distance 13: closer, but breaks a block
        board.moveTo(breaking, new OnTrack(30));
        board.assignDirection(breaking, Direction.CLOCKWISE);
        PieceId nonBreaking = new PieceId(Colour.GREEN, 2); // distance 43: farther, but no block to break
        board.moveTo(nonBreaking, new OnTrack(0));
        board.assignDirection(nonBreaking, Direction.CLOCKWISE);

        Move breakingMove = breakingStep(breaking, 30, 36);
        Move nonBreakingMove = nonBreakingStep(nonBreaking, 0, 6, false);

        assertEquals(nonBreakingMove, strategy.choose(List.of(breakingMove, nonBreakingMove), board));
    }

    @Test
    @DisplayName("A-37: among non-breaking moves, picks the one closest to home")
    void a37_picksClosestToHomeAmongNonBreakingMoves() {
        PieceId near = new PieceId(Colour.GREEN, 1); // distance 13
        board.moveTo(near, new OnTrack(30));
        board.assignDirection(near, Direction.CLOCKWISE);
        PieceId far = new PieceId(Colour.GREEN, 2); // distance 43
        board.moveTo(far, new OnTrack(0));
        board.assignDirection(far, Direction.CLOCKWISE);

        Move nearMove = nonBreakingStep(near, 30, 36, false);
        Move farMove = nonBreakingStep(far, 0, 6, false);

        assertEquals(nearMove, strategy.choose(List.of(farMove, nearMove), board));
    }

    @Test
    @DisplayName("A-40: ties between non-breaking moves break by the lowest piece number")
    void a40_tieBreaksNonBreakingMovesByLowestPieceNumber() {
        PieceId g3 = new PieceId(Colour.GREEN, 3);
        board.moveTo(g3, new OnTrack(30));
        board.assignDirection(g3, Direction.CLOCKWISE);
        PieceId g1 = new PieceId(Colour.GREEN, 1);
        board.moveTo(g1, new OnTrack(30));
        board.assignDirection(g1, Direction.CLOCKWISE);

        Move moveG3 = nonBreakingStep(g3, 30, 36, false);
        Move moveG1 = nonBreakingStep(g1, 30, 36, false);

        assertEquals(moveG1, strategy.choose(List.of(moveG3, moveG1), board));
    }

    @Test
    @DisplayName("A-66: breaks a block when every legal move breaks one, picking the closest to home")
    void a66_breaksBlockWhenEveryLegalMoveBreaksOne() {
        PieceId near = new PieceId(Colour.GREEN, 1); // distance 13
        board.moveTo(near, new OnTrack(30));
        board.assignDirection(near, Direction.CLOCKWISE);
        PieceId far = new PieceId(Colour.GREEN, 2); // distance 43
        board.moveTo(far, new OnTrack(0));
        board.assignDirection(far, Direction.CLOCKWISE);

        Move nearBreak = breakingStep(near, 30, 36);
        Move farBreak = breakingStep(far, 0, 6);

        assertEquals(nearBreak, strategy.choose(List.of(farBreak, nearBreak), board));
    }

    @Test
    @DisplayName("A-67: a capturing move has no priority over a closer non-capturing move")
    void a67_captureHasNoPriority() {
        PieceId capturer = new PieceId(Colour.GREEN, 1); // distance 43: farther, but captures
        board.moveTo(capturer, new OnTrack(0));
        board.assignDirection(capturer, Direction.CLOCKWISE);
        PieceId closer = new PieceId(Colour.GREEN, 2); // distance 13: closer, no capture
        board.moveTo(closer, new OnTrack(30));
        board.assignDirection(closer, Direction.CLOCKWISE);

        Move captureMove = capturingNonBreakingStep(capturer, 0, 6);
        Move closerMove = nonBreakingStep(closer, 30, 36, false);

        assertEquals(closerMove, strategy.choose(List.of(captureMove, closerMove), board));
    }
}
