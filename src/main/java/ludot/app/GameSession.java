package ludot.app;

import ludot.board.BoardState;
import ludot.engine.GameEngine;
import ludot.events.EventBus;

/**
 * One fully-wired, ready-to-run game: the board to inspect (before, during via a subscribed
 * listener, or after), the bus to subscribe listeners to, and the engine to drive it.
 */
public record GameSession(GameEngine engine, BoardState board, EventBus events) {

    public void run() {
        engine.run(board, board);
    }
}
