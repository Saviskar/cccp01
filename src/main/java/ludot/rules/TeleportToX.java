package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/** T-11 outcome 5: teleports to the piece's own colour's X cell (A-02). */
public final class TeleportToX implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.X;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().xIndex(pieceId.colour());
        return MysteryLanding.landAt(pieceId, index, kind(), context.board(), context.landingHandler(), context.events());
    }
}
