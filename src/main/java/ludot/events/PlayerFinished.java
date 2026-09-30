package ludot.events;

import ludot.domain.Colour;

/** Rule 11/A-41: a colour brought all 4 pieces home, taking the given place (1 = winner). */
public record PlayerFinished(Colour colour, int place) implements GameEvent {
}
