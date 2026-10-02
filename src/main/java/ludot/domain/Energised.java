package ludot.domain;

/**
 * T-12/A-32: doubles the roll for 4 full rounds after the teleport. The
 * round in which the teleport happens is not itself one of the 4 (it is
 * already under way when the teleport lands), so the countdown starts one
 * tick higher than the 4 rounds it is meant to last, and that first tick is
 * absorbed at the end of the teleport's own round.
 */
public record Energised(int roundsRemaining) implements PieceEffect {

    private static final int EFFECT_DURATION_ROUNDS = 4;
    private static final int INITIAL_ROUNDS_REMAINING = EFFECT_DURATION_ROUNDS + 1;

    public Energised() {
        this(INITIAL_ROUNDS_REMAINING);
    }

    @Override
    public int adjustSteps(int roll) {
        return roll * 2;
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public PieceEffect onRoundEnd() {
        return roundsRemaining <= 1 ? new NoEffect() : new Energised(roundsRemaining - 1);
    }
}
