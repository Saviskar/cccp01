package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;

/**
 * Resolves what happens when a piece lands on a cell (captures now; blocks
 * and mystery-cell teleports in later phases). Declared here, in {@code moves},
 * and implemented by {@code ludot.rules.LandingResolver}, so a {@link Move}
 * can trigger landing resolution without {@code moves} depending on
 * {@code rules} — {@code rules} already depends on {@code moves}, and the two
 * can't depend on each other (dependency inversion, DESIGN.md 2.2/§5 DIP).
 */
public interface LandingHandler {

    LandingResult resolveLanding(PieceId moverId, Position destination, BoardState board, EventBus events);
}
