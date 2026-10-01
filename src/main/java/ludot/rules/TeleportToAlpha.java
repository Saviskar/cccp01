package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/** T-11 outcome 1: teleports to Alpha (A-03's special cell 7). T-12's energise/sick roll is phase 4i. */
public final class TeleportToAlpha implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.ALPHA;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().alphaIndex();
        return MysteryLanding.landAt(pieceId, index, kind(), context.board(), context.landingHandler(), context.events());
    }
}
