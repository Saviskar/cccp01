package ludot.events;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/** T-11/A-29: an individual piece ended its move on the mystery cell and drew this outcome. */
public record MysteryCellTriggered(PieceId pieceId, MysteryOutcomeKind destination) implements GameEvent {
}
