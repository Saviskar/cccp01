package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.MysteryCellTriggered;
import ludot.moves.LandingHandler;
import ludot.moves.MysteryHandler;
import ludot.random.RandomPicker;

/**
 * T-11: a piece that ended its move on the mystery cell teleports to one of
 * six outcomes (A-30), which resolves A-31's landing rules there. Implements
 * {@link MysteryHandler} so a {@code Move} can trigger it without {@code moves}
 * depending on {@code rules} (see {@link MysteryHandler}'s javadoc).
 */
public final class MysteryResolver implements MysteryHandler {

    private final MysteryOutcomeFactory factory;
    private final RandomPicker picker;
    private final BoardTopology topology;
    private final LandingHandler landingHandler;

    public MysteryResolver(
            MysteryOutcomeFactory factory, RandomPicker picker, BoardTopology topology, LandingHandler landingHandler) {
        this.factory = factory;
        this.picker = picker;
        this.topology = topology;
        this.landingHandler = landingHandler;
    }

    @Override
    public boolean trigger(PieceId pieceId, BoardState board, EventBus events) {
        MysteryOutcome outcome = factory.choose(picker);
        events.publish(new MysteryCellTriggered(pieceId, outcome.kind()));
        return outcome.apply(pieceId, new MysteryContext(board, topology, events, landingHandler));
    }
}
