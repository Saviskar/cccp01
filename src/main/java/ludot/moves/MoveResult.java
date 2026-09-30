package ludot.moves;

/** Outcome of executing a {@link Move}; {@code captured} feeds T-2's bonus roll from phase 4b onward. */
public record MoveResult(boolean captured) {
}
