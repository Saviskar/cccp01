package ludot.events;

import ludot.domain.PieceId;

/**
 * T-13/A-33/A-56: the owning player rolled value 3 three times in a row while this piece was
 * Beta-restricted, so it is teleported to base (A-26). Published once per piece released, in
 * ascending piece-number order, when more than one of the colour's pieces was restricted at once.
 */
public record BriefingStreakTriggered(PieceId pieceId) implements GameEvent {
}
