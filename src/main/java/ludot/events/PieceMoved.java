package ludot.events;

import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;

/** Rule 1: a piece on the standard path (or home straight) moved by the roll value. */
public record PieceMoved(PieceId pieceId, Position from, Position to, int rollValue, Direction direction)
        implements GameEvent {
}
