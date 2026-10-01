package ludot.rules;

import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;
import ludot.events.PieceTeleported;

/** T-11 outcome 4: teleports to base, resetting the piece entirely (A-26/T-9). */
public final class TeleportToBase implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.BASE;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        context.board().resetToBase(pieceId); // A-26/T-9: a return to base always resets everything
        context.events().publish(new PieceTeleported(pieceId, kind(), new InBase(), false));
        return false; // landing in base never captures
    }
}
