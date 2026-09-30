package ludot.events;

import ludot.domain.Colour;

/** Rule 7: nothing could be moved with this roll; the dice passes to the next player (A-44). */
public record NoLegalMove(Colour colour, int rollValue) implements GameEvent {
}
