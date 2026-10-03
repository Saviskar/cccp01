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

/** A-36: Red's aggressive, capture-first behaviour. */
class RedStrategyTest {

    private final RedStrategy strategy = new RedStrategy(new MoveRanking());
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    private static Move nonCapturingStep(PieceId id, int origin, int destination, boolean formsBlock) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                false, formsBlock, false, false, false);
    }

    private static Move capturingStep(PieceId id, int origin, int destination) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, Direction.CLOCKWISE,
                true, false, false, false, false);
    }

    @Test
    @DisplayName("A-36: captures an opponent piece when a capturing move is legal")
    void a36_capturesOpponentPieceWhenPossible() {
        PieceId victim = new PieceId(Colour.GREEN, 1);
        board.moveTo(victim, new OnTrack(10));
        board.assignDirection(victim, Direction.CLOCKWISE);

        Move capture = capturingStep(new PieceId(Colour.RED, 1), 4, 10);
        Move harmless = nonCapturingStep(new PieceId(Colour.RED, 2), 20, 26, false);

        assertEquals(capture, strategy.choose(List.of(harmless, capture), board));
    }

    @Test
    @DisplayName("A-36: among several captures, prioritises the opponent piece closest to its own home")
    void a36_prioritisesCaptureClosestToOpponentsOwnHome() {
        PieceId green = new PieceId(Colour.GREEN, 1); // distance 33 from Green's own home
        board.moveTo(green, new OnTrack(10));
        board.assignDirection(green, Direction.CLOCKWISE);
        PieceId yellow = new PieceId(Colour.YELLOW, 1); // distance 7 from Yellow's own home
        board.moveTo(yellow, new OnTrack(49));
        board.assignDirection(yellow, Direction.CLOCKWISE);

        Move captureGreen = capturingStep(new PieceId(Colour.RED, 1), 40, 10);
        Move captureYellow = capturingStep(new PieceId(Colour.RED, 2), 43, 49);

        assertEquals(captureYellow, strategy.choose(List.of(captureGreen, captureYellow), board));
    }

    @Test
    @DisplayName("A-62: a multi-piece capture is ranked by its closest captured member, not by count")
    void a62_multiPieceCaptureRankedByClosestCapturedMember() {
        PieceId g1 = new PieceId(Colour.GREEN, 1); // distance 33
        board.moveTo(g1, new OnTrack(10));
        board.assignDirection(g1, Direction.CLOCKWISE);
        PieceId g2 = new PieceId(Colour.GREEN, 2); // distance 31 (closer than g1)
        board.moveTo(g2, new OnTrack(10));
        board.assignDirection(g2, Direction.COUNTERCLOCKWISE);
        board.recordApproachCrossing(g2);
        PieceId blue = new PieceId(Colour.BLUE, 1); // distance 32: strictly between g2 (31) and g1 (33)
        board.moveTo(blue, new OnTrack(37));
        board.assignDirection(blue, Direction.CLOCKWISE);

        Move blockCapture = new BlockMove(
                List.of(new PieceId(Colour.RED, 1), new PieceId(Colour.RED, 2)),
                new OnTrack(4), new OnTrack(10), 6, 3, Direction.CLOCKWISE, true, false);
        Move singleCapture = capturingStep(new PieceId(Colour.RED, 3), 31, 37);

        // Ranking by the closest captured member (31) picks the block capture over the single
        // capture at 32 — ranking by the farthest member (33) or by count would pick wrongly.
        assertEquals(blockCapture, strategy.choose(List.of(singleCapture, blockCapture), board));
    }

    @Test
    @DisplayName("A-40: ties between captures break by the lowest Red piece number")
    void a36_a40_tieBreaksCapturesByLowestRedPieceNumber() {
        PieceId yellow = new PieceId(Colour.YELLOW, 1); // distance 15
        board.moveTo(yellow, new OnTrack(41));
        board.assignDirection(yellow, Direction.CLOCKWISE);
        PieceId green = new PieceId(Colour.GREEN, 1); // distance 15, same as yellow
        board.moveTo(green, new OnTrack(28));
        board.assignDirection(green, Direction.CLOCKWISE);

        Move captureYellow = capturingStep(new PieceId(Colour.RED, 3), 35, 41);
        Move captureGreen = capturingStep(new PieceId(Colour.RED, 1), 22, 28);

        assertEquals(captureGreen, strategy.choose(List.of(captureYellow, captureGreen), board));
    }

    @Test
    @DisplayName("A-36: brings a piece out of base when no capture is possible and a base-exit is legal")
    void a36_bringsPieceFromBaseWhenNoCaptureAndBaseExitAvailable() {
        PieceId onTrack = new PieceId(Colour.RED, 2);
        board.moveTo(onTrack, new OnTrack(20)); // distance 10: very close, but still not preferred
        board.assignDirection(onTrack, Direction.CLOCKWISE);

        Move baseExit = new EnterFromBase(new PieceId(Colour.RED, 1), new OnTrack(26), false, false);
        Move moveExistingPiece = nonCapturingStep(onTrack, 20, 26, false);

        assertEquals(baseExit, strategy.choose(List.of(moveExistingPiece, baseExit), board));
    }

    @Test
    @DisplayName("A-40: ties between base-exits break by the lowest piece number")
    void a36_a40_tieBreaksBaseExitByLowestPieceNumber() {
        Move exitR3 = new EnterFromBase(new PieceId(Colour.RED, 3), new OnTrack(26), false, false);
        Move exitR1 = new EnterFromBase(new PieceId(Colour.RED, 1), new OnTrack(26), false, false);

        assertEquals(exitR1, strategy.choose(List.of(exitR3, exitR1), board));
    }

    @Test
    @DisplayName("A-63: avoids a move that forms a block when a non-blocking alternative exists")
    void a36_a63_avoidsFormingBlockWhenNonBlockingAlternativeExists() {
        PieceId blocking = new PieceId(Colour.RED, 1); // distance 6: closer, but forms a block
        board.moveTo(blocking, new OnTrack(24));
        board.assignDirection(blocking, Direction.CLOCKWISE);
        PieceId nonBlocking = new PieceId(Colour.RED, 2); // distance 50: farther, but forms no block
        board.moveTo(nonBlocking, new OnTrack(32));
        board.assignDirection(nonBlocking, Direction.CLOCKWISE);

        Move blockingMove = nonCapturingStep(blocking, 24, 30, true);
        Move nonBlockingMove = nonCapturingStep(nonBlocking, 32, 38, false);

        assertEquals(nonBlockingMove, strategy.choose(List.of(blockingMove, nonBlockingMove), board));
    }

    @Test
    @DisplayName("A-63: forms a block when every legal move does, picking the closest mover among them")
    void a36_a63_formsBlockWhenEveryLegalMoveForms() {
        PieceId r1 = new PieceId(Colour.RED, 1); // distance 10
        board.moveTo(r1, new OnTrack(20));
        board.assignDirection(r1, Direction.CLOCKWISE);
        PieceId r2 = new PieceId(Colour.RED, 2); // distance 20 (block's lowest-numbered member)
        board.moveTo(r2, new OnTrack(10));
        board.assignDirection(r2, Direction.CLOCKWISE);
        PieceId r3 = new PieceId(Colour.RED, 3);

        Move blockingStep = nonCapturingStep(r1, 20, 26, true);
        Move blockMove = new BlockMove(List.of(r2, r3), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false, false);

        assertEquals(blockingStep, strategy.choose(List.of(blockMove, blockingStep), board));
    }

    @Test
    @DisplayName("A-36: with no capture and no base-exit, moves the piece closest to home")
    void a36_movesPieceClosestToHomeWhenNoCaptureNoBaseExit() {
        PieceId near = new PieceId(Colour.RED, 1); // distance 10
        board.moveTo(near, new OnTrack(20));
        board.assignDirection(near, Direction.CLOCKWISE);
        PieceId far = new PieceId(Colour.RED, 2); // distance 30
        board.moveTo(far, new OnTrack(0));
        board.assignDirection(far, Direction.CLOCKWISE);

        Move nearMove = nonCapturingStep(near, 20, 26, false);
        Move farMove = nonCapturingStep(far, 0, 6, false);

        assertEquals(nearMove, strategy.choose(List.of(farMove, nearMove), board));
    }

    @Test
    @DisplayName("A-36/A-64: a six with no capture and no legal base-exit falls back to closest-to-home logic")
    void a36_a64_fallsBackToClosestToHomeOnSixWithNoCaptureAndNoBaseExit() {
        // A-64: no EnterFromBase in the list at all (e.g. no base piece, or X obstructed, A-25)
        // -- the move list alone must be enough to decide, since choose() never receives the
        // roll itself. This formalizes the fallback to A-36's non-six rule for this case.
        PieceId near = new PieceId(Colour.RED, 1); // distance 10
        board.moveTo(near, new OnTrack(20));
        board.assignDirection(near, Direction.CLOCKWISE);
        PieceId far = new PieceId(Colour.RED, 2); // distance 30
        board.moveTo(far, new OnTrack(0));
        board.assignDirection(far, Direction.CLOCKWISE);

        Move nearMove = nonCapturingStep(near, 20, 26, false);
        Move farMove = nonCapturingStep(far, 0, 6, false);

        assertEquals(nearMove, strategy.choose(List.of(farMove, nearMove), board));
    }
}
