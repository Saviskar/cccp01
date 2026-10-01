package ludot.events;

import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.List;

/** T-4/A-44: a block moved as a unit; {@code pieceIds} is every member, lowest piece number first. */
public record BlockMoved(
        List<PieceId> pieceIds, Position from, Position to, int rollValue, int cellsPerPiece, Direction direction)
        implements GameEvent {

    public BlockMoved {
        pieceIds = List.copyOf(pieceIds);
    }
}
