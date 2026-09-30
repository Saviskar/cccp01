package ludot.domain;

/** Null Object: a piece with no active effect moves and behaves normally. */
public record NoEffect() implements PieceEffect {

    @Override
    public int adjustSteps(int roll) {
        return roll;
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public PieceEffect onRoundEnd() {
        return this;
    }
}
