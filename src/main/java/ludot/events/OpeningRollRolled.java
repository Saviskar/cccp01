package ludot.events;

import ludot.domain.Colour;

/** Published for every opening-roll throw, including every A-27 re-roll on a tie. */
public record OpeningRollRolled(Colour colour, int value) implements GameEvent {
}
