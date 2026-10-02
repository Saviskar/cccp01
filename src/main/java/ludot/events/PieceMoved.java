package ludot.events;

import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;

/**
 * Rule 1: a piece on the standard path (or home straight) moved by {@code units} cells — the
 * roll, or A-32's effect-adjusted value when the piece is energised or sick.
 */
public record PieceMoved(PieceId pieceId, Position from, Position to, int units, Direction direction)
        implements GameEvent {
}
