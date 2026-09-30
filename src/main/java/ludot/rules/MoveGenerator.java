package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.board.Piece;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.Position;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;
import ludot.moves.StepMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Builds every legal {@link Move} for a colour and roll (Rules 1–10). */
public final class MoveGenerator {

    // Rule 2: a piece leaves base only on a six.
    private static final int BASE_EXIT_ROLL = 6;

    private final MovementCalculator movementCalculator;

    public MoveGenerator(MovementCalculator movementCalculator) {
        this.movementCalculator = movementCalculator;
    }

    public List<Move> legalMoves(Colour colour, int roll, BoardState board, BoardTopology topology) {
        List<Move> moves = new ArrayList<>();
        for (Piece piece : board.piecesOfColour(colour)) {
            moveFor(piece, colour, roll, board, topology).ifPresent(moves::add);
        }
        return moves;
    }

    private Optional<Move> moveFor(Piece piece, Colour colour, int roll, BoardState board, BoardTopology topology) {
        return switch (piece.position()) {
            case InBase ignored -> enterFromBase(piece, colour, roll, board, topology);
            case OnTrack ignored -> stepMove(piece, colour, roll, board, topology);
            case InHomeStraight ignored -> stepMove(piece, colour, roll, board, topology);
            case AtHome ignored -> Optional.empty();
        };
    }

    private Optional<Move> enterFromBase(
            Piece piece, Colour colour, int roll, BoardState board, BoardTopology topology) {
        if (roll != BASE_EXIT_ROLL) {
            return Optional.empty();
        }
        int xIndex = topology.xIndex(colour);
        Optional<Colour> occupant = board.colourAt(xIndex);
        if (occupant.isPresent() && occupant.get() == colour) {
            return Optional.empty(); // Rule 7: can't land on an own-colour piece.
        }
        boolean captures = occupant.isPresent(); // a lone opponent, since own-colour is excluded above.
        return Optional.of(new EnterFromBase(piece.id(), new OnTrack(xIndex), captures));
    }

    private Optional<Move> stepMove(Piece piece, Colour colour, int roll, BoardState board, BoardTopology topology) {
        // A-12: a piece off base always has a direction; T-1 (phase 4a) is what lets it be counterclockwise.
        Direction direction = piece.originalDirection().orElseThrow();
        RouteResult result = movementCalculator.walk(
                piece.position(), roll, colour, direction, piece.ccwApproachCrossings(), topology);
        if (!(result instanceof RouteResult.Reachable reachable)) {
            return Optional.empty(); // Rule 10: overshoot is illegal.
        }
        Position destination = reachable.destination();
        boolean crossesApproach = reachable.crossedApproachWithoutEntering(); // A-08
        if (destination instanceof OnTrack onTrack) {
            Optional<Colour> occupant = board.colourAt(onTrack.index());
            if (occupant.isPresent() && occupant.get() == colour) {
                return Optional.empty(); // Rule 7: can't land on an own-colour piece.
            }
            boolean captures = occupant.isPresent();
            return Optional.of(
                    new StepMove(piece.id(), piece.position(), destination, roll, direction, captures, crossesApproach));
        }
        // A-10: home-straight cells allow own-colour sharing and never hold an opponent.
        return Optional.of(
                new StepMove(piece.id(), piece.position(), destination, roll, direction, false, crossesApproach));
    }
}
