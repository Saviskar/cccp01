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
import ludot.random.RecordingPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A-39/A-71: Blue's cyclic, mystery-seeking/avoiding, random behaviour. */
class BlueStrategyTest {

    private static final PieceId B1 = new PieceId(Colour.BLUE, 1);
    private static final PieceId B2 = new PieceId(Colour.BLUE, 2);
    private static final PieceId B3 = new PieceId(Colour.BLUE, 3);
    private static final PieceId B4 = new PieceId(Colour.BLUE, 4);

    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    private static Move step(PieceId id, int origin, int destination, Direction direction, boolean landsOnMystery) {
        return new StepMove(id, new OnTrack(origin), new OnTrack(destination), 6, direction,
                false, false, false, false, landsOnMystery);
    }

    private static Move blockMove(List<PieceId> members, int origin, int destination, Direction direction) {
        return new BlockMove(members, new OnTrack(origin), new OnTrack(destination), 6, 3, direction, false, false);
    }

    private static Move baseExit(PieceId id) {
        return new EnterFromBase(id, new OnTrack(13), false, false);
    }

    @Test
    @DisplayName("A-39: the scheduled piece's candidates are its individual move and any block "
            + "move it belongs to, excluding other pieces' moves")
    void a39_candidatesAreIndividualAndBlockMovesForScheduledPiece() {
        board.moveTo(B1, new OnTrack(10));
        board.assignDirection(B1, Direction.CLOCKWISE);
        board.moveTo(B3, new OnTrack(20));
        board.assignDirection(B3, Direction.CLOCKWISE);

        Move otherPieceMove = step(B3, 20, 26, Direction.CLOCKWISE, false);
        Move individualMove = step(B1, 10, 16, Direction.CLOCKWISE, false);
        Move blockMove = blockMove(List.of(B1, B2), 10, 13, Direction.CLOCKWISE);

        RecordingPicker picker = new RecordingPicker(0);
        BlueStrategy strategy = new BlueStrategy(picker);

        Move chosen = strategy.choose(List.of(otherPieceMove, individualMove, blockMove), board);

        assertEquals(List.of(individualMove, blockMove), picker.lastOptions());
        assertEquals(individualMove, chosen);
    }

    @Test
    @DisplayName("A-39: a piece still in base has only base-to-X as a candidate, regardless of "
            + "direction preference (it has none assigned yet)")
    void a39_baseExitIsOnlyCandidateWhileDirectionUnset() {
        Move exit = baseExit(B1);
        RecordingPicker picker = new RecordingPicker(0);
        BlueStrategy strategy = new BlueStrategy(picker);

        Move chosen = strategy.choose(List.of(exit), board);

        assertEquals(List.of(exit), picker.lastOptions());
        assertEquals(exit, chosen);
    }

    @Test
    @DisplayName("A-39: a piece with no candidate move at all (at Home, restricted, or in base "
            + "without a six) is skipped in favour of the next piece in the cycle")
    void a39_skipsPiecesWithNoCandidateMoves() {
        // B1 contributes no move to legalMoves -- standing in for any of A-39's three reasons
        // (at Home, Beta-restricted, or in base on a non-six roll), which are indistinguishable
        // to BlueStrategy: all three simply mean MoveGenerator produced zero moves for B1.
        board.moveTo(B2, new OnTrack(5));
        board.assignDirection(B2, Direction.CLOCKWISE);
        Move onlyCandidate = step(B2, 5, 11, Direction.CLOCKWISE, false);

        BlueStrategy strategy = new BlueStrategy(new RecordingPicker(0));

        assertEquals(onlyCandidate, strategy.choose(List.of(onlyCandidate), board));
    }

    @Test
    @DisplayName("A-39: a counterclockwise piece picks a candidate that lands on the mystery "
            + "cell when one exists")
    void a39_counterclockwisePrefersMysteryLandingCandidate() {
        board.moveTo(B1, new OnTrack(10));
        board.assignDirection(B1, Direction.COUNTERCLOCKWISE);
        Move nonMystery = step(B1, 10, 4, Direction.COUNTERCLOCKWISE, false);
        Move mystery = step(B1, 10, 4, Direction.COUNTERCLOCKWISE, true);

        RecordingPicker picker = new RecordingPicker(0);
        BlueStrategy strategy = new BlueStrategy(picker);

        Move chosen = strategy.choose(List.of(nonMystery, mystery), board);

        assertEquals(List.of(mystery), picker.lastOptions());
        assertEquals(mystery, chosen);
    }

    @Test
    @DisplayName("A-39: a counterclockwise piece picks randomly among all its candidates when "
            + "none lands on the mystery cell")
    void a39_counterclockwiseRandomAmongAllWhenNoMysteryCandidate() {
        board.moveTo(B1, new OnTrack(10));
        board.assignDirection(B1, Direction.COUNTERCLOCKWISE);
        Move stepA = step(B1, 10, 4, Direction.COUNTERCLOCKWISE, false);
        Move stepB = step(B1, 10, 4, Direction.COUNTERCLOCKWISE, false);

        RecordingPicker picker = new RecordingPicker(0);
        BlueStrategy strategy = new BlueStrategy(picker);

        Move chosen = strategy.choose(List.of(stepA, stepB), board);

        assertEquals(List.of(stepA, stepB), picker.lastOptions());
        assertEquals(stepA, chosen);
    }

    @Test
    @DisplayName("A-39: a clockwise piece avoids a candidate that lands on the mystery cell "
            + "when a non-mystery candidate exists")
    void a39_clockwiseAvoidsMysteryLandingCandidate() {
        board.moveTo(B1, new OnTrack(10));
        board.assignDirection(B1, Direction.CLOCKWISE);
        Move mystery = step(B1, 10, 16, Direction.CLOCKWISE, true);
        Move nonMystery = step(B1, 10, 16, Direction.CLOCKWISE, false);

        RecordingPicker picker = new RecordingPicker(0);
        BlueStrategy strategy = new BlueStrategy(picker);

        Move chosen = strategy.choose(List.of(mystery, nonMystery), board);

        assertEquals(List.of(nonMystery), picker.lastOptions());
        assertEquals(nonMystery, chosen);
    }

    @Test
    @DisplayName("A-39: when a clockwise piece's only candidate lands on the mystery cell, Blue "
            + "tries the next piece in the cycle instead")
    void a39_clockwiseTriesNextPieceWhenOnlyCandidateIsMystery() {
        board.moveTo(B1, new OnTrack(10));
        board.assignDirection(B1, Direction.CLOCKWISE);
        board.moveTo(B2, new OnTrack(20));
        board.assignDirection(B2, Direction.CLOCKWISE);
        Move forcedMysteryB1 = step(B1, 10, 16, Direction.CLOCKWISE, true);
        Move normalB2 = step(B2, 20, 26, Direction.CLOCKWISE, false);

        BlueStrategy strategy = new BlueStrategy(new RecordingPicker(0));

        assertEquals(normalB2, strategy.choose(List.of(forcedMysteryB1, normalB2), board));
    }

    @Test
    @DisplayName("A-71: when every movable piece is clockwise and forced onto the mystery cell, "
            + "Blue takes the first such piece in the sweep, and the pointer moves to the piece "
            + "after it")
    void a71_takesFirstForcedPieceWhenEveryPieceIsMysteryForced() {
        board.moveTo(B1, new OnTrack(1));
        board.assignDirection(B1, Direction.CLOCKWISE);
        board.moveTo(B2, new OnTrack(2));
        board.assignDirection(B2, Direction.CLOCKWISE);
        board.moveTo(B3, new OnTrack(3));
        board.assignDirection(B3, Direction.CLOCKWISE);
        board.moveTo(B4, new OnTrack(4));
        board.assignDirection(B4, Direction.CLOCKWISE);
        Move forcedB1 = step(B1, 1, 7, Direction.CLOCKWISE, true);
        Move forcedB2 = step(B2, 2, 8, Direction.CLOCKWISE, true);
        Move forcedB3 = step(B3, 3, 9, Direction.CLOCKWISE, true);
        Move forcedB4 = step(B4, 4, 10, Direction.CLOCKWISE, true);

        RecordingPicker picker = new RecordingPicker(0);
        BlueStrategy strategy = new BlueStrategy(picker);

        Move firstChosen = strategy.choose(List.of(forcedB1, forcedB2, forcedB3, forcedB4), board);
        assertEquals(List.of(forcedB1), picker.lastOptions());
        assertEquals(forcedB1, firstChosen);

        // Pointer now sits one past B1 (i.e. at B2): with both B1 and B2 offering a plain
        // candidate, B2 must win because the sweep starts there, not back at B1.
        Move plainB1 = step(B1, 1, 7, Direction.CLOCKWISE, false);
        Move plainB2 = step(B2, 2, 8, Direction.CLOCKWISE, false);
        Move secondChosen = strategy.choose(List.of(plainB1, plainB2), board);
        assertEquals(plainB2, secondChosen);
    }

    @Test
    @DisplayName("A-39: the cycle pointer advances past the piece actually chosen, ready for the "
            + "next roll (including a bonus roll in the same turn)")
    void a39_pointerAdvancesAcrossCallsInNonForcedCase() {
        board.moveTo(B1, new OnTrack(1));
        board.assignDirection(B1, Direction.CLOCKWISE);
        board.moveTo(B2, new OnTrack(2));
        board.assignDirection(B2, Direction.CLOCKWISE);
        Move firstMoveB1 = step(B1, 1, 7, Direction.CLOCKWISE, false);

        BlueStrategy strategy = new BlueStrategy(new RecordingPicker(0));
        assertEquals(firstMoveB1, strategy.choose(List.of(firstMoveB1), board));

        // Pointer now sits one past B1 (i.e. at B2): with both B1 and B2 offering a plain
        // candidate, B2 must win because the sweep starts there, not back at B1.
        Move secondMoveB1 = step(B1, 1, 7, Direction.CLOCKWISE, false);
        Move secondMoveB2 = step(B2, 2, 8, Direction.CLOCKWISE, false);
        assertEquals(secondMoveB2, strategy.choose(List.of(secondMoveB1, secondMoveB2), board));
    }
}
