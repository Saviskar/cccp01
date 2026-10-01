package ludot.rules;

import ludot.events.PieceBlocked;
import ludot.moves.Move;

/**
 * T-6/A-22: the outcome of a single block member's forced break-move. A movable member gets a
 * {@link Move} to execute (a plain step, or a partial move if obstructed with any cells reachable,
 * A-52); a member whose path is immediately obstructed cannot move at all, and only the "is
 * blocked" fact applies (A-52) — there is no Move to execute.
 */
public sealed interface ForcedMoveOutcome permits ForcedMoveOutcome.Movable, ForcedMoveOutcome.DeadEnd {

    record Movable(Move move) implements ForcedMoveOutcome {
    }

    record DeadEnd(PieceBlocked blocked) implements ForcedMoveOutcome {
    }
}
