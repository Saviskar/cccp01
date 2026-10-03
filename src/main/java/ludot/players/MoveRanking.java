package ludot.players;

import ludot.board.GameView;
import ludot.domain.PieceId;
import ludot.moves.Move;

import java.util.Comparator;
import java.util.List;

/**
 * Shared "closest to home" move ranking used by strategies via composition, in place of a
 * Template Method base class (DESIGN.md §7): the four strategies share little structure beyond
 * this one comparison.
 */
public final class MoveRanking {

    /** A-40/A-51: the lowest-numbered piece among a move's movers. */
    public PieceId mover(Move move) {
        return move.pieceIds().stream().min(Comparator.comparingInt(PieceId::number)).orElseThrow();
    }

    /** A-36/A-38/A-40: the move whose mover is closest to home, ties broken by piece number. */
    public Move closestMoverToHome(List<Move> moves, GameView view) {
        return moves.stream()
                .min(Comparator.<Move>comparingInt(move -> view.distanceFromHome(mover(move)))
                        .thenComparingInt(move -> mover(move).number()))
                .orElseThrow();
    }
}
