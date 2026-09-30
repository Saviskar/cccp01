package ludot.events;

import ludot.domain.Colour;

/** Rule 4: a third consecutive six is ignored and the turn ends with no move (A-44). */
public record ThirdSixIgnored(Colour colour) implements GameEvent {
}
