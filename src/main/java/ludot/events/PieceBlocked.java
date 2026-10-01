package ludot.events;

import ludot.domain.PieceId;
import ludot.domain.Position;

/**
 * A-16/A-48: a piece's full move was obstructed by an opponent block.
 * {@code intendedDestination} is where the full roll would have taken it;
 * {@code blockingPieceId} is the lowest-numbered piece in the obstructing
 * block. Published only when the obstruction decides the turn (A-48).
 */
public record PieceBlocked(PieceId pieceId, Position from, Position intendedDestination, PieceId blockingPieceId)
        implements GameEvent {
}
