package ludot.players;

import ludot.board.GameView;
import ludot.moves.Move;

import java.util.List;

/**
 * Hand-written test fake (not a mock, per CLAUDE.md): always picks the first
 * legal move. Stands in for the real strategies until phase 6.
 */
public final class FirstLegalMoveStrategy implements PlayerStrategy {

    @Override
    public Move choose(List<Move> legalMoves, GameView view) {
        return legalMoves.get(0);
    }
}
