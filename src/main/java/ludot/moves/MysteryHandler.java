package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.PieceId;
import ludot.events.EventBus;

/**
 * Resolves T-11: a piece that ended its move on the mystery cell teleports to
 * one of six outcomes (A-30/A-31). Declared here, in {@code moves}, and
 * implemented by {@code ludot.rules.MysteryResolver}, for the same reason as
 * {@link LandingHandler}: a {@link Move} can trigger it without {@code moves}
 * depending on {@code rules} (dependency inversion, DESIGN.md 2.2/5 DIP).
 */
public interface MysteryHandler {

    /** @return whether the resolved teleport captured an opponent piece (T-2's bonus roll). */
    boolean trigger(PieceId pieceId, BoardState board, EventBus events);
}
