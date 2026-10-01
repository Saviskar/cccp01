package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/**
 * One of T-11's six teleport destinations (Factory, DESIGN.md 4.5). Each
 * implementation teleports the piece to its own fixed cell and resolves A-31's
 * landing rules there.
 */
public interface MysteryOutcome {

    MysteryOutcomeKind kind();

    /** @return whether the teleport captured an opponent piece (T-2's bonus roll). */
    boolean apply(PieceId pieceId, MysteryContext context);
}
