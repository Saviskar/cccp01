package ludot.domain;

/**
 * T-12/A-32: halves the roll (floor division) for 4 full rounds after the
 * teleport. See {@link Energised} for why the countdown starts one tick
 * higher than the 4 rounds it is meant to last.
 */
public record Sick(int roundsRemaining) implements PieceEffect {

    private static final int EFFECT_DURATION_ROUNDS = 4;
    private static final int INITIAL_ROUNDS_REMAINING = EFFECT_DURATION_ROUNDS + 1;

    public Sick() {
        this(INITIAL_ROUNDS_REMAINING);
    }

    @Override
    public int adjustSteps(int roll) {
        return roll / 2;
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public PieceEffect onRoundEnd() {
        return roundsRemaining <= 1 ? new NoEffect() : new Sick(roundsRemaining - 1);
    }
}
