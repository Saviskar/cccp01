package ludot.events;

import ludot.domain.Colour;

/**
 * A-16/A-48/A-47: no piece of this colour could make any move (every
 * obstructed piece was a dead end), so the throw is ignored and the turn
 * ends — even on a six (A-47's exception). Published once per turn, never
 * alongside {@link NoLegalMove}.
 */
public record ThrowIgnoredAfterBlock(Colour colour) implements GameEvent {
}
