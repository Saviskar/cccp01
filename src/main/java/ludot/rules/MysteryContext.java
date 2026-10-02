package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.events.EventBus;
import ludot.moves.LandingHandler;
import ludot.random.RandomPicker;

/**
 * Everything a {@link MysteryOutcome} needs to teleport a piece and resolve A-31's
 * landing rules. {@code picker} is the same injected {@link RandomPicker} used to
 * draw the outcome itself (DESIGN.md 4.6); {@link TeleportToAlpha} reuses it for
 * T-12's energised/sick roll (A-32).
 */
public record MysteryContext(
        BoardState board, BoardTopology topology, EventBus events, LandingHandler landingHandler,
        RandomPicker picker) {
}
