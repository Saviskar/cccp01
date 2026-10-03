package ludot.events;

import ludot.domain.Colour;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.Optional;

/**
 * T-11/A-31: the resolved result of a {@link MysteryCellTriggered} draw.
 * {@code finalPosition} is where the piece actually ended up; {@code redirectedToBase}
 * is true when an opponent block occupied {@code destination}, sending the piece to
 * base instead of landing there (A-31). {@code blockingColour} names that block's
 * colour when {@code redirectedToBase} is true (A-60); empty otherwise.
 */
public record PieceTeleported(
        PieceId pieceId, MysteryOutcomeKind destination, Position finalPosition, boolean redirectedToBase,
        Optional<Colour> blockingColour) implements GameEvent {
}
