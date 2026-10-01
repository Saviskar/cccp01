package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Walks a piece's route one cell at a time (Rule 9/10), so later phases can
 * inspect or branch on individual cells along the way: T-1 counterclockwise
 * routing and extra laps (phase 4a), block obstruction (phase 4c), and T-7
 * eligibility gating the approach-to-home-straight transition (phase 4f).
 */
public final class MovementCalculator {

    // A-08: a counterclockwise piece may enter the home straight once it has crossed
    // its Approach this many times without entering (i.e. from the second crossing onward).
    private static final int CROSSINGS_REQUIRED_FOR_HOME_STRAIGHT = 1;

    /**
     * @param ccwApproachCrossings the piece's crossing count (A-08) entering this move; irrelevant
     *                             for {@link Direction#CLOCKWISE}, which has no eligibility gate.
     * @param board                consulted for opponent blocks along the way (T-3/A-16); own-colour
     *                             blocks never obstruct (A-15), and home-straight cells are never
     *                             checked, since no opposing piece can ever be there (A-10).
     */
    public RouteResult walk(
            Position from, int steps, Colour colour, Direction direction, int ccwApproachCrossings,
            BoardTopology topology, BoardState board) {
        WalkState state = new WalkState(from, ccwApproachCrossings, false, Optional.empty());
        for (int i = 0; i < steps; i++) {
            if (state.current() instanceof AtHome) {
                // A-49: an overshoot takes priority over any obstruction already found — the move
                // is illegal either way, and there is no valid intended destination to report.
                return new RouteResult.Overshoot();
            }
            state = advance(state, i, colour, direction, topology, board);
        }
        return state.toRouteResult();
    }

    // Performs one step of the walk, carrying forward whether any obstruction has already
    // been found and how many Approach crossings have accumulated so far (A-08).
    private WalkState advance(
            WalkState state, int cellsWalkedSoFar, Colour colour, Direction direction, BoardTopology topology,
            BoardState board) {
        StepOutcome outcome = stepOnce(state.current(), colour, direction, state.crossings(), topology);
        Optional<ObstructionPoint> obstruction = state.firstObstruction().isPresent()
                ? state.firstObstruction()
                : detectObstruction(
                        state.current(), outcome, colour, board, cellsWalkedSoFar, state.crossedDuringWalk());
        boolean crossedNow = outcome.crossedApproachWithoutEntering();
        int crossings = state.crossings() + (crossedNow ? 1 : 0);
        boolean crossedDuringWalk = state.crossedDuringWalk() || crossedNow;
        return new WalkState(outcome.position(), crossings, crossedDuringWalk, obstruction);
    }

    // Accumulates the walk's progress across iterations, so walk() and advance() pass one
    // value instead of four separate locals.
    private record WalkState(
            Position current, int crossings, boolean crossedDuringWalk, Optional<ObstructionPoint> firstObstruction) {

        RouteResult toRouteResult() {
            if (firstObstruction.isPresent()) {
                ObstructionPoint point = firstObstruction.get();
                return new RouteResult.Obstructed(
                        point.position(), point.cellsWalked(), point.crossedApproachWithoutEntering(), current,
                        point.blockingPieceId());
            }
            return new RouteResult.Reachable(current, crossedDuringWalk);
        }
    }

    // T-3/A-16: records the first opponent block a step lands on, so the walk can report it
    // once fully known (A-49 may still override with an overshoot further along).
    private Optional<ObstructionPoint> detectObstruction(
            Position current, StepOutcome outcome, Colour colour, BoardState board, int cellsWalked,
            boolean crossedDuringWalk) {
        if (!(outcome.position() instanceof OnTrack(int idx)) || !isOpponentBlock(idx, colour, board)) {
            return Optional.empty();
        }
        return Optional.of(
                new ObstructionPoint(current, cellsWalked, crossedDuringWalk, lowestNumberedOccupant(idx, board)));
    }

    private boolean isOpponentBlock(int trackIndex, Colour colour, BoardState board) {
        return board.isBlock(trackIndex) && board.colourAt(trackIndex).orElseThrow() != colour;
    }

    private PieceId lowestNumberedOccupant(int trackIndex, BoardState board) {
        List<PieceId> occupants = board.piecesAt(trackIndex);
        return occupants.stream().min(Comparator.comparingInt(PieceId::number)).orElseThrow();
    }

    private record ObstructionPoint(Position position, int cellsWalked, boolean crossedApproachWithoutEntering,
                                     PieceId blockingPieceId) {
    }

    private StepOutcome stepOnce(
            Position current, Colour colour, Direction direction, int ccwApproachCrossings, BoardTopology topology) {
        return switch (current) {
            case OnTrack(int index) -> stepOnTrack(index, colour, direction, ccwApproachCrossings, topology);
            case InHomeStraight(int cell) -> new StepOutcome(cell == BoardTopology.HOME_STRAIGHT_LENGTH - 1
                    ? new AtHome()
                    : new InHomeStraight(cell + 1), false);
            case AtHome ignored -> throw new IllegalStateException("Cannot step further once at Home");
            case InBase ignored -> throw new IllegalArgumentException(
                    "MovementCalculator does not walk from base; use EnterFromBase for Rule 2");
        };
    }

    private StepOutcome stepOnTrack(
            int index, Colour colour, Direction direction, int ccwApproachCrossings, BoardTopology topology) {
        if (index != topology.approachIndex(colour)) {
            return new StepOutcome(new OnTrack(topology.step(index, direction)), false);
        }
        // Rule 9: clockwise always enters. A-08: counterclockwise only from its second
        // crossing onward; the first crossing continues around the standard track instead.
        boolean entersHomeStraight =
                direction == Direction.CLOCKWISE || ccwApproachCrossings >= CROSSINGS_REQUIRED_FOR_HOME_STRAIGHT;
        if (entersHomeStraight) {
            return new StepOutcome(new InHomeStraight(0), false);
        }
        return new StepOutcome(new OnTrack(topology.step(index, direction)), true);
    }

    private record StepOutcome(Position position, boolean crossedApproachWithoutEntering) {
    }
}
