package ludot.moves;

import ludot.board.BoardState;
import ludot.events.EventBus;

/** Everything a {@link Move} needs to execute itself and publish the resulting events. */
public record MoveContext(BoardState board, EventBus events, LandingHandler landingHandler) {
}
