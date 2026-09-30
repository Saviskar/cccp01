package ludot.board;

import ludot.domain.Colour;
import ludot.domain.PieceId;

import java.util.List;
import java.util.Optional;

/**
 * Read-only view of the board that a {@code PlayerStrategy} depends on, instead
 * of the mutable {@link BoardState} (Interface Segregation, DESIGN.md 4.6/5).
 * {@link BoardState} implements this directly.
 */
public interface GameView {

    Piece piece(PieceId id);

    List<Piece> piecesOfColour(Colour colour);

    List<PieceId> piecesAt(int trackIndex);

    List<PieceId> piecesInHomeStraight(Colour colour, int cell);

    boolean isBlock(int trackIndex);

    Optional<Colour> colourAt(int trackIndex);

    int countInBase(Colour colour);

    int countOnBoard(Colour colour);

    int countAtHome(Colour colour);
}
