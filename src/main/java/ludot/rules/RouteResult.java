package ludot.rules;

import ludot.domain.PieceId;
import ludot.domain.Position;

/** Outcome of walking a piece's route by a number of steps (Rule 9/10). */
public sealed interface RouteResult permits RouteResult.Reachable, RouteResult.Overshoot, RouteResult.Obstructed {

    /**
     * The steps land exactly on {@code destination}.
     *
     * @param crossedApproachWithoutEntering A-08: whether this walk passed a counterclockwise
     *                                       piece's Approach cell without entering the home
     *                                       straight, and so must record a fresh crossing.
     */
    record Reachable(Position destination, boolean crossedApproachWithoutEntering) implements RouteResult {
    }

    /** Rule 10: the steps would move the piece past Home; there is no bounce-back (A-09). */
    record Overshoot() implements RouteResult {
    }

    /**
     * T-3/A-16: an opponent block stopped the walk before the full roll was used up.
     *
     * @param lastReachablePosition          the cell immediately before the block (the partial-move destination)
     * @param cellsWalked                    how many cells were travelled to reach it; 0 means the block is adjacent
     * @param crossedApproachWithoutEntering A-08, for the partial segment actually walked
     * @param intendedDestination            A-48: where the full, unobstructed roll would have landed
     * @param blockingPieceId                A-48: the lowest-numbered piece in the obstructing block
     */
    record Obstructed(
            Position lastReachablePosition, int cellsWalked, boolean crossedApproachWithoutEntering,
            Position intendedDestination, PieceId blockingPieceId) implements RouteResult {
    }
}
