package ludot.events;

import ludot.domain.PieceId;
import ludot.domain.Position;

/** Rule 6: a piece captured an opponent piece, which returns to base. */
public record PieceCaptured(PieceId capturerId, Position landedOn, PieceId capturedId) implements GameEvent {
}
