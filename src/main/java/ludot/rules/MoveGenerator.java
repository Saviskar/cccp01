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
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.PieceBlocked;
import ludot.moves.BlockMove;
import ludot.moves.EnterFromBase;
import ludot.moves.Move;
import ludot.moves.PartialMove;
import ludot.moves.StepMove;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Builds every legal {@link Move} for a colour and roll (Rules 1–10, T-3, T-4). */
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
        fullMoves.addAll(blockMoves(colour, roll, board, topology)); // T-4: offered alongside individual moves
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
        // T-5/A-21 fact: true when this piece's cell is currently a block of its own colour.
        boolean breaksBlock = piece.position() instanceof OnTrack(int idx) && board.isBlock(idx);
        RouteResult result = movementCalculator.walk(
                piece.position(), roll, colour, direction, piece.ccwApproachCrossings(), piece.captureCount(),
                topology, board);
        switch (result) {
            case RouteResult.Overshoot ignored -> { } // Rule 10: overshoot is illegal.
            case RouteResult.Obstructed obs ->
                    obstructed.add(new ObstructedAttempt(piece, roll, direction, obs, breaksBlock));
            case RouteResult.Reachable(Position destination, boolean crossed) ->
                    fullMoves.add(buildStepMove(piece, destination, roll, direction, crossed, breaksBlock, board));
        }
    }

    private Move buildStepMove(
            Piece piece, Position destination, int roll, Direction direction, boolean crossed, boolean breaksBlock,
            BoardState board) {
        if (destination instanceof OnTrack(int idx)) {
            OccupancyOutcome occupancy = occupancyOutcome(idx, piece.id().colour(), board);
            return new StepMove(
                    piece.id(), piece.position(), destination, roll, direction, occupancy.captures(),
                    occupancy.formsBlock(), breaksBlock, crossed, landsOnMystery(idx, board));
        }
        // A-10: home-straight cells allow own-colour sharing without forming a block, and never hold an opponent.
        return new StepMove(
                piece.id(), piece.position(), destination, roll, direction, false, false, breaksBlock, crossed,
                false);
    }

    // A-29: only a standard-track destination can be the mystery cell.
    private boolean landsOnMystery(int trackIndex, BoardState board) {
        return board.mysteryCellLocation().map(location -> location == trackIndex).orElse(false);
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
                occupancy.captures(), occupancy.formsBlock(), attempt.breaksBlock(),
                route.crossedApproachWithoutEntering(), landsOnMystery(idx, board));
    }

    private OccupancyOutcome occupancyOutcome(int trackIndex, Colour colour, BoardState board) {
        Optional<Colour> occupant = board.colourAt(trackIndex);
        if (occupant.isEmpty()) {
            return new OccupancyOutcome(false, false);
        }
        if (occupant.get() == colour) {
            return new OccupancyOutcome(false, true); // A-14: lands on own colour, forms a block.
        }
        // An opponent occupant here is always a capture: for a single piece's walk, a block would
        // already have obstructed it (so the occupant is a lone piece); for a block move's landing
        // cell, buildBlockMove already confirmed any opponent block there is the same size (T-8/A-20).
        return new OccupancyOutcome(true, false);
    }

    // T-4/A-17: a block move is offered for every own-colour block, independently of the
    // per-piece moves above (a colour can own more than one block at once, e.g. 2+2).
    private List<Move> blockMoves(Colour colour, int roll, BoardState board, BoardTopology topology) {
        List<Move> moves = new ArrayList<>();
        for (int cell : board.blockCellsOf(colour)) {
            List<PieceId> members = board.piecesAt(cell).stream()
                    .sorted(Comparator.comparingInt(PieceId::number)) // A-40/A-51: lowest piece number first.
                    .toList();
            int cellsPerPiece = roll / members.size();
            if (cellsPerPiece == 0) {
                continue; // A-17: a zero-cell block move is illegal.
            }
            Direction direction = blockDirection(members, cell, colour, board, topology);
            buildBlockMove(members, cell, roll, cellsPerPiece, direction, colour, board, topology)
                    .ifPresent(moves::add);
        }
        return moves;
    }

    // A-17: the members' shared direction if they agree; otherwise the farthest-from-home
    // member's direction, tie broken to clockwise. Comparing each direction's best distance
    // handles both cases uniformly: when every member agrees, only one direction has any
    // candidate distance at all.
    private Direction blockDirection(
            List<PieceId> members, int cell, Colour colour, BoardState board, BoardTopology topology) {
        int clockwiseBest = Integer.MIN_VALUE;
        int counterclockwiseBest = Integer.MIN_VALUE;
        for (PieceId id : members) {
            Piece piece = board.piece(id);
            Direction direction = piece.originalDirection().orElseThrow();
            int distance = topology.distanceFromHome(colour, direction, cell, piece.ccwApproachCrossings());
            if (direction == Direction.CLOCKWISE) {
                clockwiseBest = Math.max(clockwiseBest, distance);
            } else {
                counterclockwiseBest = Math.max(counterclockwiseBest, distance);
            }
        }
        return counterclockwiseBest > clockwiseBest ? Direction.COUNTERCLOCKWISE : Direction.CLOCKWISE;
    }

    // A-50: an opponent block anywhere on the path before the landing cell makes the whole block
    // move illegal — no partial block moves exist. T-8/A-20: a same-size opponent block on the
    // landing cell is a legal capture instead; a different-size block there still obstructs, as
    // does an opponent block of any size encountered earlier on the path. A-08: a counterclockwise
    // walk that steps past (leaves) the shared Approach cell records a crossing for every member
    // (A-18: a block move never enters the home straight, so this is always a non-entering crossing).
    private Optional<Move> buildBlockMove(
            List<PieceId> members, int originCell, int roll, int cellsPerPiece, Direction direction, Colour colour,
            BoardState board, BoardTopology topology) {
        int approach = topology.approachIndex(colour);
        int idx = originCell;
        boolean crossesApproach = false;
        for (int step = 0; step < cellsPerPiece; step++) {
            if (direction == Direction.COUNTERCLOCKWISE && idx == approach) {
                crossesApproach = true;
            }
            idx = topology.step(idx, direction);
            boolean isLandingCell = step == cellsPerPiece - 1;
            if (board.isBlock(idx) && board.colourAt(idx).orElseThrow() != colour) {
                if (!isLandingCell || board.piecesAt(idx).size() != members.size()) {
                    return Optional.empty();
                }
            }
        }
        OccupancyOutcome occupancy = occupancyOutcome(idx, colour, board);
        return Optional.of(new BlockMove(
                members, new OnTrack(originCell), new OnTrack(idx), roll, cellsPerPiece, direction,
                occupancy.captures(), crossesApproach));
    }

    // T-6/A-22: the Move a piece makes if forced to walk `units` cells in `direction` — the forced
    // break after a third consecutive six, rather than a move chosen by a strategy from a roll's
    // legal list. Reuses the same walk/landing logic as an ordinary roll, via buildStepMove and
    // buildPartialMove below, instead of duplicating it. A-52's dead-end sub-case (the obstructing
    // block is immediately adjacent) has no Move to execute, so the result distinguishes it from a
    // movable outcome.
    public ForcedMoveOutcome forcedMove(
            Piece piece, int units, Direction direction, boolean breaksBlock, BoardState board,
            BoardTopology topology) {
        Colour colour = piece.id().colour();
        RouteResult result = movementCalculator.walk(
                piece.position(), units, colour, direction, piece.ccwApproachCrossings(), piece.captureCount(),
                topology, board);
        return switch (result) {
            case RouteResult.Overshoot ignored -> throw new IllegalStateException(
                    "T-6 forced move cannot overshoot: on-track distance to home is always >= 6 (A-05), "
                            + "matching A-22's maximum 6-unit share");
            case RouteResult.Obstructed obs when obs.cellsWalked() == 0 -> new ForcedMoveOutcome.DeadEnd(
                    new PieceBlocked(piece.id(), piece.position(), obs.intendedDestination(), obs.blockingPieceId()));
            case RouteResult.Obstructed obs -> new ForcedMoveOutcome.Movable(
                    buildPartialMove(new ObstructedAttempt(piece, units, direction, obs, breaksBlock), board));
            case RouteResult.Reachable(Position destination, boolean crossed) -> new ForcedMoveOutcome.Movable(
                    buildStepMove(piece, destination, units, direction, crossed, breaksBlock, board));
        };
    }

    private record ObstructedAttempt(
            Piece piece, int roll, Direction direction, RouteResult.Obstructed route, boolean breaksBlock) {
    }

    private record OccupancyOutcome(boolean captures, boolean formsBlock) {
    }
}
