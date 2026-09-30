package ludot.events;

import ludot.domain.PieceId;
import ludot.domain.Position;

/** One piece's location, as reported in the round-end status ("Piece [Name] -> L1 or Base or Home."). */
public record PieceLocation(PieceId pieceId, Position position) {
}
