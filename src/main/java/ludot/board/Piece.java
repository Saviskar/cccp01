package ludot.board;

import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.NoEffect;
import ludot.domain.PieceEffect;
import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.Optional;

/**
 * One piece's mutable state. Every mutator is package-private, so
 * {@link BoardState} is the only public way to change a piece. This keeps
 * BoardState's track/home-straight occupancy always in sync with each
 * piece's own position — a caller outside {@code ludot.board} cannot call
 * {@code moveTo} or {@code resetToBase} directly and desync the two, and the
 * compiler enforces it rather than a comment (DESIGN.md 3.4, 3.5).
 */
public final class Piece {

    private final PieceId id;
    private Position position;
    private Optional<Direction> originalDirection;
    private int captureCount;
    private int ccwApproachCrossings;
    private PieceEffect effect;

    Piece(PieceId id) {
        this.id = id;
        this.position = new InBase();
        this.originalDirection = Optional.empty();
        this.captureCount = 0;
        this.ccwApproachCrossings = 0;
        this.effect = new NoEffect();
    }

    public PieceId id() {
        return id;
    }

    public Position position() {
        return position;
    }

    public Optional<Direction> originalDirection() {
        return originalDirection;
    }

    public int captureCount() {
        return captureCount;
    }

    public int ccwApproachCrossings() {
        return ccwApproachCrossings;
    }

    public PieceEffect effect() {
        return effect;
    }

    void moveTo(Position newPosition) {
        this.position = newPosition;
    }

    // A-12: set once by the base-to-X coin toss; T-14 (Gamma) later overwrites it permanently.
    void assignDirection(Direction direction) {
        this.originalDirection = Optional.of(direction);
    }

    // A-08: counts crossings of the Approach cell while moving counterclockwise.
    void recordApproachCrossing() {
        this.ccwApproachCrossings++;
    }

    // A-07/T-7: eligibility for the home straight requires at least one capture.
    void recordCapture() {
        this.captureCount++;
    }

    // A-45: a new Alpha or Beta effect replaces any existing one.
    void applyEffect(PieceEffect effect) {
        this.effect = effect;
    }

    // T-9/A-26: any return to base resets every piece of state.
    void resetToBase() {
        this.position = new InBase();
        this.originalDirection = Optional.empty();
        this.captureCount = 0;
        this.ccwApproachCrossings = 0;
        this.effect = new NoEffect();
    }
}
