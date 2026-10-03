package ludot.players;

import ludot.board.GameView;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;

import java.util.Comparator;
import java.util.List;

/**
 * A-38: Yellow always prioritises winning. On a six with a piece in base, it brings one out
 * unconditionally -- no block-forming exception, unlike Green (A-65(1)). Otherwise it prioritises
 * a capture by a piece that still needs one (capture count 0), eligible even on a block move if
 * any member needs a capture (A-68), with no distance-based ranking among qualifying captures
 * (A-69). Failing that, it moves the piece closest to home, treating a block move like any other
 * move (A-70).
 */
public final class YellowStrategy implements PlayerStrategy {

    private final MoveRanking ranking;

    public YellowStrategy(MoveRanking ranking) {
        this.ranking = ranking;
    }

    @Override
    public Move choose(List<Move> legalMoves, GameView view) {
        List<Move> baseExits = legalMoves.stream().filter(move -> move instanceof EnterFromBase).toList();
        if (!baseExits.isEmpty()) {
            return lowestNumberedMover(baseExits);
        }
        List<Move> needyCaptures = legalMoves.stream()
                .filter(Move::capturesSomething)
                .filter(move -> anyMoverNeedsCapture(move, view))
                .toList();
        if (!needyCaptures.isEmpty()) {
            // A-69: no distance-based ranking among qualifying captures.
            return lowestNumberedMover(needyCaptures);
        }
        // A-70: a block move is ranked here like any other move.
        return ranking.closestMoverToHome(legalMoves, view);
    }

    // A-38/A-68: "needs captures" means capture count 0; a block move qualifies if any member
    // does, since a block capture credits every member (A-19).
    private boolean anyMoverNeedsCapture(Move move, GameView view) {
        return move.pieceIds().stream().anyMatch(id -> view.piece(id).captureCount() == 0);
    }

    // A-40: ties go to the lowest piece number.
    private Move lowestNumberedMover(List<Move> moves) {
        return moves.stream()
                .min(Comparator.comparingInt(move -> ranking.mover(move).number()))
                .orElseThrow();
    }
}
