package ludot.events;

import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;

/** A-16: a piece obstructed by a block moved only as far as the cell before it. */
public record PiecePartiallyMoved(PieceId pieceId, Position from, Position to, int cellsMoved, Direction direction)
        implements GameEvent {
}
