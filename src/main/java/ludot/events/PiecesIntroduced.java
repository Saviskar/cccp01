package ludot.events;

import ludot.domain.Colour;
import ludot.domain.PieceId;

import java.util.List;

/** Published once per colour at game start: "The red player has four (04) pieces...". */
public record PiecesIntroduced(Colour colour, List<PieceId> pieceIds) implements GameEvent {

    private static final int PIECES_PER_COLOUR = 4;

    public PiecesIntroduced {
        if (pieceIds.size() != PIECES_PER_COLOUR) {
            throw new IllegalArgumentException("A colour has exactly 4 pieces, got " + pieceIds.size());
        }
        pieceIds = List.copyOf(pieceIds);
    }
}
