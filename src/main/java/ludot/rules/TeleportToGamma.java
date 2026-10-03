package ludot.rules;

import ludot.domain.Direction;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceId;
import ludot.events.GammaDirectionReversed;
import ludot.events.GammaRerouteTriggered;

/**
 * T-11 outcome 3: teleports to Gamma (A-03's special cell 44), then resolves T-14's
 * direction change (A-34). A clockwise piece permanently turns counterclockwise; a
 * counterclockwise piece is rerouted on to Beta, where the Beta effect (A-33) applies
 * as part of the same mystery-cell chain (A-58).
 */
public final class TeleportToGamma implements MysteryOutcome {

    private final MysteryOutcome betaOutcome;

    public TeleportToGamma(MysteryOutcome betaOutcome) {
        this.betaOutcome = betaOutcome;
    }

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.GAMMA;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().gammaIndex();
        LandingOutcome outcome = MysteryLanding.landAt(
                pieceId, index, kind(), context.board(), context.landingHandler(), context.events());
        // A-31/A-58: an opponent block redirects the piece to base instead of landing on
        // Gamma, ending the chain before T-14 is ever consulted.
        if (outcome.redirectedToBase()) {
            return outcome.captured();
        }
        Direction direction = context.board().piece(pieceId).originalDirection().orElseThrow();
        if (direction == Direction.CLOCKWISE) {
            reverseDirection(pieceId, context);
            return outcome.captured();
        }
        // A-58: evaluate the reroute unconditionally — a capture at Gamma must not short-circuit
        // past the Beta leg, even though only one bonus-roll signal comes out the far end.
        boolean capturedAtBeta = rerouteToBeta(pieceId, context);
        return outcome.captured() || capturedAtBeta;
    }

    private void reverseDirection(PieceId pieceId, MysteryContext context) {
        // A-34: permanent — becomes the piece's new original direction for T-5/A-21.
        // A-58: its counterclockwise crossing count (A-08) is untouched by this switch.
        context.board().assignDirection(pieceId, Direction.COUNTERCLOCKWISE);
        context.events().publish(new GammaDirectionReversed(pieceId));
    }

    private boolean rerouteToBeta(PieceId pieceId, MysteryContext context) {
        context.events().publish(new GammaRerouteTriggered(pieceId));
        // A-34/A-58: the Beta effect (A-33) applies, since this chain started at a mystery cell.
        return betaOutcome.apply(pieceId, context);
    }
}
