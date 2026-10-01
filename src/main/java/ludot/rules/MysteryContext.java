package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.events.EventBus;
import ludot.moves.LandingHandler;

/** Everything a {@link MysteryOutcome} needs to teleport a piece and resolve A-31's landing rules. */
public record MysteryContext(BoardState board, BoardTopology topology, EventBus events, LandingHandler landingHandler) {
}
