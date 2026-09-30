package ludot.events;

import ludot.domain.Colour;

/** Published for every in-game dice roll (distinct from the opening-roll wording). */
public record DiceRolled(Colour colour, int value) implements GameEvent {
}
