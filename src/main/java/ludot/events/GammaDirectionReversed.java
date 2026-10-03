package ludot.events;

import ludot.domain.PieceId;

/** T-14/A-34: a clockwise piece teleported to Gamma permanently becomes counterclockwise. */
public record GammaDirectionReversed(PieceId pieceId) implements GameEvent {
}
