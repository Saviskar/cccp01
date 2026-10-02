package ludot.events;

import ludot.domain.AlphaEffectKind;
import ludot.domain.PieceId;

/** T-12/A-32: a piece that landed on Alpha drew this effect. */
public record AlphaEffectAssigned(PieceId pieceId, AlphaEffectKind kind) implements GameEvent {
}
