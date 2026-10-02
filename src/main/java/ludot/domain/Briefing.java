package ludot.domain;

/**
 * T-13/A-33: a piece teleported to Beta cannot move at all for 4 full rounds after the teleport.
 * See {@link Energised} for why the countdown starts one tick higher than the 4 rounds it is
 * meant to last.
 */
public record Briefing(int roundsRemaining) implements PieceEffect {

    private static final int EFFECT_DURATION_ROUNDS = 4;
    private static final int INITIAL_ROUNDS_REMAINING = EFFECT_DURATION_ROUNDS + 1;

    public Briefing() {
        this(INITIAL_ROUNDS_REMAINING);
    }

    @Override
    public int adjustSteps(int roll) {
        return roll; // unreachable in practice: canMove() is false, so MoveGenerator never calls this.
    }

    @Override
    public boolean canMove() {
        return false;
    }

    @Override
    public PieceEffect onRoundEnd() {
        return roundsRemaining <= 1 ? new NoEffect() : new Briefing(roundsRemaining - 1);
    }
}
