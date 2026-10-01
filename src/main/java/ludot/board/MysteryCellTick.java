package ludot.board;

/**
 * A-28: the mystery cell's state after one round-end tick, when it is active
 * (returned by {@link MysteryCell#onRoundEnd}). {@code roundsRemaining} is one
 * of 4, 3, 2, 1 — never 0, since the cell respawns immediately instead.
 */
public record MysteryCellTick(int location, int roundsRemaining, boolean justSpawned) {
}
