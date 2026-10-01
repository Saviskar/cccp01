package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/** T-11 outcome 6: teleports to the piece's own colour's Approach cell (A-02). */
public final class TeleportToApproach implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.APPROACH;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().approachIndex(pieceId.colour());
        return MysteryLanding.landAt(pieceId, index, kind(), context.board(), context.landingHandler(), context.events());
    }
}
