package ludot.events;

import ludot.domain.Direction;
import ludot.domain.PieceId;

/** T-1/A-12: the coin toss result assigning a piece's direction after it reaches X. */
public record PieceDirectionAssigned(PieceId pieceId, Direction direction) implements GameEvent {
}
