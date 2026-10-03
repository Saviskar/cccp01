package ludot.players;

import ludot.board.GameView;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.moves.Move;
import ludot.random.RandomPicker;

import java.util.List;
import java.util.Optional;

/**
 * A-39: Blue is a random player that cycles through B1..B4, skipping any piece with no candidate
 * move (at Home, Beta-restricted, or in base without a six). A counterclockwise piece prefers a
 * candidate that lands on the mystery cell; a clockwise piece avoids one, trying the next piece
 * in the cycle instead -- unless every movable piece is clockwise and forced onto the mystery
 * cell, in which case A-71 takes the first such piece found. Remaining choices are random, via
 * the injected {@link RandomPicker}, never by {@link MoveRanking} (Blue has no distance
 * preference).
 */
public final class BlueStrategy implements PlayerStrategy {

    private static final int FIRST_PIECE_NUMBER = 1;
    private static final int PIECES_PER_COLOUR = 4;

    private final RandomPicker randomPicker;
    private int nextPieceNumber = FIRST_PIECE_NUMBER; // A-39 cycle pointer

    // A-71: a piece found clockwise with every candidate landing on the mystery cell.
    private record ForcedPiece(PieceId id, List<Move> candidates) {
    }

    public BlueStrategy(RandomPicker randomPicker) {
        this.randomPicker = randomPicker;
    }

    @Override
    public Move choose(List<Move> legalMoves, GameView view) {
        Optional<ForcedPiece> firstForced = Optional.empty();

        for (int offset = 0; offset < PIECES_PER_COLOUR; offset++) {
            int pieceNumber = wrap(nextPieceNumber + offset);
            PieceId id = new PieceId(Colour.BLUE, pieceNumber);
            List<Move> candidates = candidatesFor(id, legalMoves);
            if (candidates.isEmpty()) {
                continue; // A-39: at Home / restricted / in base without a six
            }

            Optional<List<Move>> pool = selectPool(id, candidates, view);
            if (pool.isPresent()) {
                Move chosen = randomPicker.pick(pool.get());
                nextPieceNumber = wrap(pieceNumber + 1);
                return chosen;
            }
            if (firstForced.isEmpty()) {
                firstForced = Optional.of(new ForcedPiece(id, candidates));
            }
        }

        // A-71: every movable piece was clockwise and forced onto the mystery cell.
        ForcedPiece forced = firstForced.orElseThrow(
                () -> new IllegalStateException("BlueStrategy.choose() called with no movable piece"));
        Move chosen = randomPicker.pick(forced.candidates());
        nextPieceNumber = wrap(forced.id().number() + 1);
        return chosen;
    }

    // A-39: a piece's candidates are its individual move, any block move it belongs to, and
    // base-to-X on a six -- all identifiable purely by membership in a move's pieceIds().
    private List<Move> candidatesFor(PieceId id, List<Move> legalMoves) {
        return legalMoves.stream().filter(move -> move.pieceIds().contains(id)).toList();
    }

    // Returns the pool to pick from, or empty if the piece must be skipped (A-39's
    // clockwise-forced case).
    private Optional<List<Move>> selectPool(PieceId id, List<Move> candidates, GameView view) {
        Optional<Direction> direction = view.piece(id).originalDirection();
        if (direction.isEmpty()) {
            return Optional.of(candidates); // A-12: still in base; the only candidate is EnterFromBase
        }
        List<Move> mysteryLanding = candidates.stream().filter(Move::landsOnMystery).toList();
        if (direction.get() == Direction.COUNTERCLOCKWISE) {
            return Optional.of(mysteryLanding.isEmpty() ? candidates : mysteryLanding);
        }
        List<Move> nonMystery = candidates.stream().filter(move -> !move.landsOnMystery()).toList();
        return nonMystery.isEmpty() ? Optional.empty() : Optional.of(nonMystery);
    }

    private static int wrap(int pieceNumber) {
        return ((pieceNumber - 1) % PIECES_PER_COLOUR) + 1;
    }
}
