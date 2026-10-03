package ludot.players;

import ludot.board.GameView;
import ludot.moves.BlockMove;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;

import java.util.Comparator;
import java.util.List;

/**
 * A-37: Green prioritises winning by blocking. On a six with a piece in base, it prefers a move
 * that forms a new block over bringing a piece out of base (A-65(1)). Otherwise it always takes
 * a legal block move (A-65(2)/T-4), then prefers any move that does not break a block, ranked
 * closest to home (A-65(3)/A-66), and only breaks a block when every legal move does
 * (A-65(4)/A-66). Captures carry no special priority (A-67).
 */
public final class GreenStrategy implements PlayerStrategy {

    private final MoveRanking ranking;

    public GreenStrategy(MoveRanking ranking) {
        this.ranking = ranking;
    }

    @Override
    public Move choose(List<Move> legalMoves, GameView view) {
        List<Move> baseExits = legalMoves.stream().filter(move -> move instanceof EnterFromBase).toList();
        if (!baseExits.isEmpty()) {
            return chooseOnSix(legalMoves, baseExits);
        }
        List<Move> blockMoves = legalMoves.stream().filter(move -> move instanceof BlockMove).toList();
        if (!blockMoves.isEmpty()) {
            return lowestNumberedMover(blockMoves);
        }
        List<Move> nonBreaking = legalMoves.stream().filter(move -> !move.breaksBlock()).toList();
        if (!nonBreaking.isEmpty()) {
            return ranking.closestMoverToHome(nonBreaking, view);
        }
        // A-66: every legal move breaks a block.
        return ranking.closestMoverToHome(legalMoves, view);
    }

    // A-65(1): a move that forms a new block (not a block move, which is already one) beats
    // bringing a piece out of base.
    private Move chooseOnSix(List<Move> legalMoves, List<Move> baseExits) {
        List<Move> newBlockForming = legalMoves.stream()
                .filter(move -> !(move instanceof BlockMove) && move.formsBlock())
                .toList();
        if (!newBlockForming.isEmpty()) {
            return lowestNumberedMover(newBlockForming);
        }
        return lowestNumberedMover(baseExits);
    }

    // A-40: remaining ties go to the lowest piece number.
    private Move lowestNumberedMover(List<Move> moves) {
        return moves.stream()
                .min(Comparator.comparingInt(move -> ranking.mover(move).number()))
                .orElseThrow();
    }
}
