package ludot.domain;

/**
 * A timed effect on a piece (Alpha/Beta outcomes, T-12/T-13). Implementations
 * replace conditionals on effect type with polymorphism (State pattern).
 */
public interface PieceEffect {

    /** Applies the effect to a raw dice roll to get the actual movement distance. */
    int adjustSteps(int roll);

    /** Whether a piece with this effect is allowed to move at all. */
    boolean canMove();

    /** Called once per round; returns the effect to carry into the next round. */
    PieceEffect onRoundEnd();
}
