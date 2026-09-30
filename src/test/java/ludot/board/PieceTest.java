package ludot.board;

import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.NoEffect;
import ludot.domain.OnTrack;
import ludot.domain.PieceEffect;
import ludot.domain.PieceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PieceTest {

    private Piece piece;

    @BeforeEach
    void setUp() {
        piece = new Piece(new PieceId(Colour.RED, 1));
    }

    @Test
    @DisplayName("a new piece starts in base with no direction, no captures, no crossings and no effect")
    void newPieceHasDefaultState() {
        assertEquals(new InBase(), piece.position());
        assertEquals(Optional.empty(), piece.originalDirection());
        assertEquals(0, piece.captureCount());
        assertEquals(0, piece.ccwApproachCrossings());
        assertEquals(new NoEffect(), piece.effect());
    }

    @Test
    @DisplayName("id is fixed at construction")
    void idIsFixed() {
        assertEquals(new PieceId(Colour.RED, 1), piece.id());
    }

    @Test
    @DisplayName("moveTo changes only the position")
    void moveToChangesPosition() {
        piece.moveTo(new OnTrack(26));
        assertEquals(new OnTrack(26), piece.position());
    }

    @Test
    @DisplayName("A-12: assignDirection sets the original direction")
    void a12_assignDirectionSetsDirection() {
        piece.assignDirection(Direction.COUNTERCLOCKWISE);
        assertEquals(Optional.of(Direction.COUNTERCLOCKWISE), piece.originalDirection());
    }

    @Test
    @DisplayName("A-08: recordApproachCrossing increments the counter")
    void a08_recordApproachCrossingIncrements() {
        piece.recordApproachCrossing();
        piece.recordApproachCrossing();
        assertEquals(2, piece.ccwApproachCrossings());
    }

    @Test
    @DisplayName("A-07/T-7: recordCapture increments the capture count")
    void a07_recordCaptureIncrements() {
        piece.recordCapture();
        assertEquals(1, piece.captureCount());
    }

    @Test
    @DisplayName("A-45: applyEffect replaces the active effect")
    void a45_applyEffectReplacesEffect() {
        PieceEffect effect = new NoEffect();
        piece.applyEffect(effect);
        assertEquals(effect, piece.effect());
    }

    @Test
    @DisplayName("A-26/T-9: resetToBase restores every field to its construction default")
    void a26_resetToBaseRestoresEveryField() {
        piece.moveTo(new OnTrack(10));
        piece.assignDirection(Direction.CLOCKWISE);
        piece.recordApproachCrossing();
        piece.recordCapture();
        piece.applyEffect(new NoEffect());

        piece.resetToBase();

        assertEquals(new InBase(), piece.position());
        assertEquals(Optional.empty(), piece.originalDirection());
        assertEquals(0, piece.captureCount());
        assertEquals(0, piece.ccwApproachCrossings());
        assertEquals(new NoEffect(), piece.effect());
    }
}
