package ludot.events;

/**
 * Published once, by {@code GameEngine.run()} as its first action, before {@code
 * PiecesIntroduced}: announces the seed a run used (A-72), given or defaulted by {@code Main}.
 */
public record SeedSelected(long seed) implements GameEvent {
}
