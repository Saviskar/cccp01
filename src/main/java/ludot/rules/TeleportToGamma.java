package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;

/**
 * T-11 outcome 3: teleports to Gamma (A-03's special cell 44). T-14's direction
 * change / reroute-to-Beta for a counterclockwise piece is phase 4k.
 */
public final class TeleportToGamma implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.GAMMA;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().gammaIndex();
        return MysteryLanding.landAt(pieceId, index, kind(), context.board(), context.landingHandler(), context.events())
                .captured();
    }
}
