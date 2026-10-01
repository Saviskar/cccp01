package ludot.events;

/**
 * A-28: published once per round, while the mystery cell is active, with the
 * number of rounds it has left at its current location (4, 3, 2, 1).
 */
public record MysteryCellStatusReported(int location, int roundsRemaining) implements GameEvent {
}
