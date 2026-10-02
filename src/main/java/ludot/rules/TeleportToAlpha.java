package ludot.rules;

import ludot.domain.AlphaEffectKind;
import ludot.domain.Energised;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.PieceEffect;
import ludot.domain.PieceId;
import ludot.domain.Sick;
import ludot.events.AlphaEffectAssigned;

import java.util.List;

/**
 * T-11 outcome 1: teleports to Alpha (A-03's special cell 7), then rolls T-12's
 * energised/sick effect (A-32) if the piece actually ended up there.
 */
public final class TeleportToAlpha implements MysteryOutcome {

    @Override
    public MysteryOutcomeKind kind() {
        return MysteryOutcomeKind.ALPHA;
    }

    @Override
    public boolean apply(PieceId pieceId, MysteryContext context) {
        int index = context.topology().alphaIndex();
        LandingOutcome outcome = MysteryLanding.landAt(
                pieceId, index, kind(), context.board(), context.landingHandler(), context.events());
        // A-31: an opponent block redirects the piece to base instead of landing on Alpha, in
        // which case T-12's effect never applies.
        if (!outcome.redirectedToBase()) {
            rollEffect(pieceId, context);
        }
        return outcome.captured();
    }

    private void rollEffect(PieceId pieceId, MysteryContext context) {
        AlphaEffectKind kind = context.picker().pick(List.of(AlphaEffectKind.ENERGISED, AlphaEffectKind.SICK));
        PieceEffect effect = switch (kind) {
            case ENERGISED -> new Energised();
            case SICK -> new Sick();
        };
        context.board().applyEffect(pieceId, effect);
        context.events().publish(new AlphaEffectAssigned(pieceId, kind));
    }
}
