package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/** T-11 outcome 2: teleports to Beta (A-03's special cell 25). T-13's briefing restriction is phase 4j. */
public final class TeleportToBeta implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.BETA;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().betaIndex();
        return MysteryLanding.landAt(pieceId, index, kind(), context.board(), context.landingHandler(), context.events())
                .captured();
    }
}
