package ludot.events;

import ludot.domain.Colour;

/** Published once the opening roll (A-27) has a single winner. */
public record OpeningRollWinnerDetermined(Colour colour) implements GameEvent {
}
