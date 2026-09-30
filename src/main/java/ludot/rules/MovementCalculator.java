package ludot.rules;

import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.Position;

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
     */
    public RouteResult walk(
            Position from, int steps, Colour colour, Direction direction, int ccwApproachCrossings,
            BoardTopology topology) {
        Position current = from;
        int crossings = ccwApproachCrossings;
        boolean crossedDuringWalk = false;
        for (int i = 0; i < steps; i++) {
            if (current instanceof AtHome) {
                // Rule 10/A-09: already home before the roll is used up — no bounce-back.
                return new RouteResult.Overshoot();
            }
            StepOutcome outcome = stepOnce(current, colour, direction, crossings, topology);
            current = outcome.position();
            if (outcome.crossedApproachWithoutEntering()) {
                crossings++;
                crossedDuringWalk = true;
            }
        }
        return new RouteResult.Reachable(current, crossedDuringWalk);
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
