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
import ludot.events.PieceBlocked;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;
import ludot.moves.PartialMove;
import ludot.moves.StepMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Builds every legal {@link Move} for a colour and roll (Rules 1–10, T-3). */
public final class MoveGenerator {

    // Rule 2: a piece leaves base only on a six.
    private static final int BASE_EXIT_ROLL = 6;

    private final MovementCalculator movementCalculator;

    public MoveGenerator(MovementCalculator movementCalculator) {
        this.movementCalculator = movementCalculator;
    }

    public MoveGenerationResult legalMoves(Colour colour, int roll, BoardState board, BoardTopology topology) {
        List<Move> fullMoves = new ArrayList<>();
        List<ObstructedAttempt> obstructed = new ArrayList<>();
        for (Piece piece : board.piecesOfColour(colour)) {
            collect(piece, colour, roll, board, topology, fullMoves, obstructed);
        }
        // A-16/A-48: a partial move (or its dead-end report) is only ever produced
        // as a last resort, when the colour has no other legal full move.
        if (!fullMoves.isEmpty()) {
            return new MoveGenerationResult(fullMoves, List.of());
        }
        return fallback(obstructed, board);
    }

    private void collect(
            Piece piece, Colour colour, int roll, BoardState board, BoardTopology topology,
            List<Move> fullMoves, List<ObstructedAttempt> obstructed) {
        switch (piece.position()) {
            case InBase ignored -> enterFromBase(piece, colour, roll, board, topology).ifPresent(fullMoves::add);
            case OnTrack ignored -> collectStepOrObstruction(piece, colour, roll, board, topology, fullMoves, obstructed);
            case InHomeStraight ignored ->
                    collectStepOrObstruction(piece, colour, roll, board, topology, fullMoves, obstructed);
            case AtHome ignored -> { }
        }
    }

    private Optional<Move> enterFromBase(
            Piece piece, Colour colour, int roll, BoardState board, BoardTopology topology) {
        if (roll != BASE_EXIT_ROLL) {
            return Optional.empty();
        }
        int xIndex = topology.xIndex(colour);
        Optional<Colour> occupant = board.colourAt(xIndex);
        if (occupant.isEmpty()) {
            return Optional.of(new EnterFromBase(piece.id(), new OnTrack(xIndex), false, false));
        }
        if (occupant.get() == colour) {
            return Optional.of(new EnterFromBase(piece.id(), new OnTrack(xIndex), false, true)); // A-14/A-25: forms a block
        }
        if (board.isBlock(xIndex)) {
            return Optional.empty(); // A-25: an opponent block on X makes the move illegal.
        }
        return Optional.of(new EnterFromBase(piece.id(), new OnTrack(xIndex), true, false)); // single opponent: capture
    }

    private void collectStepOrObstruction(
            Piece piece, Colour colour, int roll, BoardState board, BoardTopology topology,
            List<Move> fullMoves, List<ObstructedAttempt> obstructed) {
        // A-12: a piece off base always has a direction; T-1 (phase 4a) is what lets it be counterclockwise.
        Direction direction = piece.originalDirection().orElseThrow();
        RouteResult result = movementCalculator.walk(
                piece.position(), roll, colour, direction, piece.ccwApproachCrossings(), topology, board);
        switch (result) {
            case RouteResult.Overshoot ignored -> { } // Rule 10: overshoot is illegal.
            case RouteResult.Obstructed obs -> obstructed.add(new ObstructedAttempt(piece, roll, direction, obs));
            case RouteResult.Reachable(Position destination, boolean crossed) ->
                    fullMoves.add(buildStepMove(piece, destination, roll, direction, crossed, board));
        }
    }

    private Move buildStepMove(
            Piece piece, Position destination, int roll, Direction direction, boolean crossed, BoardState board) {
        if (destination instanceof OnTrack(int idx)) {
            OccupancyOutcome occupancy = occupancyOutcome(idx, piece.id().colour(), board);
            return new StepMove(
                    piece.id(), piece.position(), destination, roll, direction, occupancy.captures(),
                    occupancy.formsBlock(), crossed);
        }
        // A-10: home-straight cells allow own-colour sharing without forming a block, and never hold an opponent.
        return new StepMove(piece.id(), piece.position(), destination, roll, direction, false, false, crossed);
    }

    private MoveGenerationResult fallback(List<ObstructedAttempt> obstructed, BoardState board) {
        List<Move> legalMoves = new ArrayList<>();
        List<PieceBlocked> deadEnds = new ArrayList<>();
        for (ObstructedAttempt attempt : obstructed) {
            RouteResult.Obstructed route = attempt.route();
            if (route.cellsWalked() > 0) {
                legalMoves.add(buildPartialMove(attempt, board));
            } else {
                // A-16: the block is directly adjacent, so this piece cannot move at all.
                deadEnds.add(new PieceBlocked(
                        attempt.piece().id(), attempt.piece().position(), route.intendedDestination(),
                        route.blockingPieceId()));
            }
        }
        return new MoveGenerationResult(legalMoves, deadEnds);
    }

    private Move buildPartialMove(ObstructedAttempt attempt, BoardState board) {
        RouteResult.Obstructed route = attempt.route();
        Position destination = route.lastReachablePosition();
        if (!(destination instanceof OnTrack(int idx))) {
            throw new IllegalStateException("A partial move's stopping cell must be on the standard track: " + destination);
        }
        OccupancyOutcome occupancy = occupancyOutcome(idx, attempt.piece().id().colour(), board);
        return new PartialMove(
                attempt.piece().id(), attempt.piece().position(), destination, route.intendedDestination(),
                route.blockingPieceId(), attempt.roll(), route.cellsWalked(), attempt.direction(),
                occupancy.captures(), occupancy.formsBlock(), route.crossedApproachWithoutEntering());
    }

    private OccupancyOutcome occupancyOutcome(int trackIndex, Colour colour, BoardState board) {
        Optional<Colour> occupant = board.colourAt(trackIndex);
        if (occupant.isEmpty()) {
            return new OccupancyOutcome(false, false);
        }
        if (occupant.get() == colour) {
            return new OccupancyOutcome(false, true); // A-14: lands on own colour, forms a block.
        }
        return new OccupancyOutcome(true, false); // a lone opponent — a block would already have obstructed the walk.
    }

    private record ObstructedAttempt(Piece piece, int roll, Direction direction, RouteResult.Obstructed route) {
    }

    private record OccupancyOutcome(boolean captures, boolean formsBlock) {
    }
}
