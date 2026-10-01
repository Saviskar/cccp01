package ludot.events;

/** T-10/A-28: the mystery cell (re)spawned at {@code location}, staying for four rounds. */
public record MysterySpawned(int location) implements GameEvent {
}
