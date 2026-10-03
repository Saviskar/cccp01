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

/** A-38/A-68/A-69/A-70: Yellow's win-first behaviour. */
class YellowStrategyTest {

    private final YellowStrategy strategy = new YellowStrategy(new MoveRanking());
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    private static Move nonCapturingStep(PieceId id, int origin, int destination) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                false, false, false, false, false);
    }

    private static Move capturingStep(PieceId id, int origin, int destination) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                true, false, false, false, false);
    }

    @Test
    @DisplayName("A-38: a base exit is taken unconditionally on a six, even over a capture")
    void a38_baseExitTakenUnconditionallyOnSix() {
        Move baseExit = new EnterFromBase(new PieceId(Colour.YELLOW, 1), new OnTrack(0), false, false);
        PieceId capturer = new PieceId(Colour.YELLOW, 2);
        board.moveTo(capturer, new OnTrack(10));
        board.assignDirection(capturer, Direction.CLOCKWISE);
        Move captureMove = capturingStep(capturer, 10, 16);

        assertEquals(baseExit, strategy.choose(List.of(captureMove, baseExit), board));
    }

    @Test
    @DisplayName("A-40: ties between base-exits break by the lowest piece number")
    void a40_tieBreaksBaseExitsByLowestPieceNumber() {
        Move exitY3 = new EnterFromBase(new PieceId(Colour.YELLOW, 3), new OnTrack(0), false, false);
        Move exitY1 = new EnterFromBase(new PieceId(Colour.YELLOW, 1), new OnTrack(0), false, false);

        assertEquals(exitY1, strategy.choose(List.of(exitY3, exitY1), board));
    }

    @Test
    @DisplayName("A-38: captures by a piece that needs captures when available")
    void a38_capturesByNeedyPieceWhenAvailable() {
        PieceId capturer = new PieceId(Colour.YELLOW, 1); // capture count 0, farther from home
        board.moveTo(capturer, new OnTrack(0));
        board.assignDirection(capturer, Direction.CLOCKWISE);
        PieceId closer = new PieceId(Colour.YELLOW, 2); // closer to home but no capture
        board.moveTo(closer, new OnTrack(40));
        board.assignDirection(closer, Direction.CLOCKWISE);

        Move captureMove = capturingStep(capturer, 0, 6);
        Move closerMove = nonCapturingStep(closer, 40, 46);

        assertEquals(captureMove, strategy.choose(List.of(closerMove, captureMove), board));
    }

    @Test
    @DisplayName("A-38: \"needs captures\" means capture count 0 -- a capture by an already-capturing "
            + "piece is not prioritised")
    void a38_pieceNeedsCapturesMeansCaptureCountZero() {
        PieceId alreadyCaptured = new PieceId(Colour.YELLOW, 1); // capture count 1, farther from home
        board.moveTo(alreadyCaptured, new OnTrack(0));
        board.assignDirection(alreadyCaptured, Direction.CLOCKWISE);
        board.recordCapture(alreadyCaptured);
        PieceId closer = new PieceId(Colour.YELLOW, 2); // closer to home, no capture available
        board.moveTo(closer, new OnTrack(40));
        board.assignDirection(closer, Direction.CLOCKWISE);

        Move captureMove = capturingStep(alreadyCaptured, 0, 6);
        Move closerMove = nonCapturingStep(closer, 40, 46);

        assertEquals(closerMove, strategy.choose(List.of(captureMove, closerMove), board));
    }

    @Test
    @DisplayName("A-68: a capturing block move is eligible if any member needs a capture")
    void a68_blockMoveCaptureEligibleIfAnyMemberNeedsCapture() {
        PieceId needy = new PieceId(Colour.YELLOW, 1);
        PieceId satisfied = new PieceId(Colour.YELLOW, 2);
        board.moveTo(needy, new OnTrack(10));
        board.assignDirection(needy, Direction.CLOCKWISE);
        board.moveTo(satisfied, new OnTrack(10));
        board.assignDirection(satisfied, Direction.CLOCKWISE);
        board.recordCapture(satisfied);

        PieceId closer = new PieceId(Colour.YELLOW, 3);
        board.moveTo(closer, new OnTrack(45));
        board.assignDirection(closer, Direction.CLOCKWISE);

        Move blockCapture = new BlockMove(List.of(needy, satisfied), new OnTrack(10), new OnTrack(13), 6, 3,
                Direction.CLOCKWISE, true, false);
        Move closerMove = nonCapturingStep(closer, 45, 51);

        assertEquals(blockCapture, strategy.choose(List.of(closerMove, blockCapture), board));
    }

    @Test
    @DisplayName("A-68: a capturing block move is not eligible when no member needs a capture")
    void a68_blockMoveCaptureNotEligibleWhenNoMemberNeedsCapture() {
        PieceId member1 = new PieceId(Colour.YELLOW, 1);
        PieceId member2 = new PieceId(Colour.YELLOW, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.recordCapture(member1);
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.recordCapture(member2);

        PieceId closer = new PieceId(Colour.YELLOW, 3);
        board.moveTo(closer, new OnTrack(45));
        board.assignDirection(closer, Direction.CLOCKWISE);

        Move blockCapture = new BlockMove(List.of(member1, member2), new OnTrack(10), new OnTrack(13), 6, 3,
                Direction.CLOCKWISE, true, false);
        Move closerMove = nonCapturingStep(closer, 45, 51);

        assertEquals(closerMove, strategy.choose(List.of(blockCapture, closerMove), board));
    }

    @Test
    @DisplayName("A-69: ties between qualifying captures break by the lowest piece number, not "
            + "by the captured piece's distance from home")
    void a69_tieBreaksQualifyingCapturesByLowestPieceNumber() {
        PieceId captorHigh = new PieceId(Colour.YELLOW, 3);
        PieceId captorLow = new PieceId(Colour.YELLOW, 1);
        board.moveTo(captorHigh, new OnTrack(0));
        board.assignDirection(captorHigh, Direction.CLOCKWISE);
        board.moveTo(captorLow, new OnTrack(20));
        board.assignDirection(captorLow, Direction.CLOCKWISE);

        // Both capture; captorHigh's victim would be closer to its own home (irrelevant under A-69).
        Move captureByHigh = capturingStep(captorHigh, 0, 6);
        Move captureByLow = capturingStep(captorLow, 20, 26);

        assertEquals(captureByLow, strategy.choose(List.of(captureByHigh, captureByLow), board));
    }

    @Test
    @DisplayName("A-38: moves the piece closest to home when no capture is available")
    void a38_movesClosestToHomeWhenNoCaptureAvailable() {
        PieceId near = new PieceId(Colour.YELLOW, 1); // distance 13
        board.moveTo(near, new OnTrack(30));
        board.assignDirection(near, Direction.CLOCKWISE);
        PieceId far = new PieceId(Colour.YELLOW, 2); // distance 43
        board.moveTo(far, new OnTrack(0));
        board.assignDirection(far, Direction.CLOCKWISE);

        Move nearMove = nonCapturingStep(near, 30, 36);
        Move farMove = nonCapturingStep(far, 0, 6);

        assertEquals(nearMove, strategy.choose(List.of(farMove, nearMove), board));
    }

    @Test
    @DisplayName("A-40: ties between closest-to-home moves break by the lowest piece number")
    void a40_tieBreaksClosestToHomeByLowestPieceNumber() {
        PieceId y3 = new PieceId(Colour.YELLOW, 3);
        board.moveTo(y3, new OnTrack(30));
        board.assignDirection(y3, Direction.CLOCKWISE);
        PieceId y1 = new PieceId(Colour.YELLOW, 1);
        board.moveTo(y1, new OnTrack(30));
        board.assignDirection(y1, Direction.CLOCKWISE);

        Move moveY3 = nonCapturingStep(y3, 30, 36);
        Move moveY1 = nonCapturingStep(y1, 30, 36);

        assertEquals(moveY1, strategy.choose(List.of(moveY3, moveY1), board));
    }

    @Test
    @DisplayName("A-70: a non-capturing block move is ranked like any other move by its "
            + "lowest-numbered member, with no avoidance or preference")
    void a70_blockMoveRankedLikeAnyOtherMove() {
        PieceId blockMember = new PieceId(Colour.YELLOW, 1); // distance 13: closer, via a block move
        board.moveTo(blockMember, new OnTrack(30));
        board.assignDirection(blockMember, Direction.CLOCKWISE);
        PieceId blockPartner = new PieceId(Colour.YELLOW, 2);
        board.moveTo(blockPartner, new OnTrack(30));
        board.assignDirection(blockPartner, Direction.CLOCKWISE);
        PieceId farSingle = new PieceId(Colour.YELLOW, 3); // distance 43: farther, single piece
        board.moveTo(farSingle, new OnTrack(0));
        board.assignDirection(farSingle, Direction.CLOCKWISE);

        Move blockMove = new BlockMove(List.of(blockMember, blockPartner), new OnTrack(30), new OnTrack(36), 6, 3,
                Direction.CLOCKWISE, false, false);
        Move farMove = nonCapturingStep(farSingle, 0, 6);

        assertEquals(blockMove, strategy.choose(List.of(farMove, blockMove), board));
    }
}
