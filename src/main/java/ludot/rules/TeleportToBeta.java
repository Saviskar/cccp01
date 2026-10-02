package ludot.rules;

import ludot.domain.Briefing;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;
import ludot.events.BriefingAssigned;

/**
 * T-11 outcome 2: teleports to Beta (A-03's special cell 25), then applies T-13's briefing
 * restriction (A-33) if the piece actually ended up there.
 */
public final class TeleportToBeta implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.BETA;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().betaIndex();
        LandingOutcome outcome = MysteryLanding.landAt(
                pieceId, index, kind(), context.board(), context.landingHandler(), context.events());
        // A-31: an opponent block redirects the piece to base instead of landing on Beta, in
        // which case T-13's effect never applies.
        if (!outcome.redirectedToBase()) {
            assignBriefing(pieceId, context);
        }
        return outcome.captured();
    }

    private void assignBriefing(PieceId pieceId, MysteryContext context) {
        context.board().applyEffect(pieceId, new Briefing());
        context.events().publish(new BriefingAssigned(pieceId));
    }
}
