package ludot.rules;

import ludot.domain.Position;

/** Outcome of walking a piece's route by a number of steps (Rule 9/10). */
public sealed interface RouteResult permits RouteResult.Reachable, RouteResult.Overshoot {

    /** The steps land exactly on {@code destination}. */
    record Reachable(Position destination) implements RouteResult {
    }

    /** Rule 10: the steps would move the piece past Home; there is no bounce-back (A-09). */
    record Overshoot() implements RouteResult {
    }
}
