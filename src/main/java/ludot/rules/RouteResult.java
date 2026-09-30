package ludot.rules;

import ludot.domain.Position;

/** Outcome of walking a piece's route by a number of steps (Rule 9/10). */
public sealed interface RouteResult permits RouteResult.Reachable, RouteResult.Overshoot {

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
}
