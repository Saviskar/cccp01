package ludot.players;

import ludot.board.GameView;
import ludot.domain.Colour;
import ludot.domain.OnTrack;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;

import java.util.Comparator;
import java.util.List;

/**
 * A-36: Red is aggressive -- it prioritises capturing (ranked by the captured piece closest to
 * its own home, A-62), then brings a piece out of base when no capture is possible. On a six
 * with no legal base-exit, or on any other roll, it then avoids forming a block (A-63) unless
 * every legal move does (A-64), preferring whichever piece is closest to home.
 */
public final class RedStrategy implements PlayerStrategy {

    private final MoveRanking ranking;

    public RedStrategy(MoveRanking ranking) {
        this.ranking = ranking;
    }

    @Override
    public Move choose(List<Move> legalMoves, GameView view) {
        List<Move> captures = legalMoves.stream().filter(Move::capturesSomething).toList();
        if (!captures.isEmpty()) {
            return bestCapture(captures, view);
        }
        // A-25: EnterFromBase only ever appears in the list when the roll was a six and X is
        // free, so its presence alone tells Red a base-exit is legal -- no roll parameter needed.
        List<Move> baseExits = legalMoves.stream().filter(move -> move instanceof EnterFromBase).toList();
        if (!baseExits.isEmpty()) {
            return baseExits.stream()
                    .min(Comparator.comparingInt(move -> ranking.mover(move).number()))
                    .orElseThrow();
        }
        // A-64: this is also reached for a six with no legal base-exit (e.g. no piece left in
        // base), in which case Red falls back to A-36's non-six rule below.
        // A-63: a block move always leaves its pieces in block form, so it's treated the same as
        // a move that newly forms one -- avoided unless every legal move does.
        List<Move> nonBlocking = legalMoves.stream().filter(move -> !move.formsBlock()).toList();
        List<Move> candidates = nonBlocking.isEmpty() ? legalMoves : nonBlocking;
        return ranking.closestMoverToHome(candidates, view);
    }

    // A-36/A-62: the capture whose closest captured opponent is nearest its own home; ties break
    // by Red's own lowest-numbered mover (A-40).
    private Move bestCapture(List<Move> captures, GameView view) {
        return captures.stream()
                .min(Comparator.<Move>comparingInt(move -> closestCapturedDistance(move, view))
                        .thenComparingInt(move -> ranking.mover(move).number()))
                .orElseThrow();
    }

    private int closestCapturedDistance(Move move, GameView view) {
        int trackIndex = destinationTrackIndex(move);
        Colour moverColour = ranking.mover(move).colour();
        return view.piecesAt(trackIndex).stream()
                .filter(id -> id.colour() != moverColour)
                .mapToInt(view::distanceFromHome)
                .min()
                .orElseThrow(); // A-11/A-36: a capturing move always has an opponent occupant.
    }

    private int destinationTrackIndex(Move move) {
        if (move.destination() instanceof OnTrack(int idx)) {
            return idx;
        }
        throw new IllegalStateException("A-11: a capturing move always lands on the standard track");
    }
}
