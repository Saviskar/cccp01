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

    public RouteResult walk(Position from, int steps, Colour colour, Direction direction, BoardTopology topology) {
        Position current = from;
        for (int i = 0; i < steps; i++) {
            if (current instanceof AtHome) {
                // Rule 10/A-09: already home before the roll is used up — no bounce-back.
                return new RouteResult.Overshoot();
            }
            current = stepOnce(current, colour, direction, topology);
        }
        return new RouteResult.Reachable(current);
    }

    private Position stepOnce(Position current, Colour colour, Direction direction, BoardTopology topology) {
        if (direction == Direction.COUNTERCLOCKWISE) {
            // T-1 (phase 4a) adds real counterclockwise walking, including extra laps
            // before a piece is allowed into the home straight (A-08).
            throw new UnsupportedOperationException("Counterclockwise walking is added in phase 4a");
        }
        return switch (current) {
            case OnTrack(int index) -> index == topology.approachIndex(colour)
                    // Rule 9: passing the Approach cell enters the home straight. Traditional
                    // Ludo has no eligibility gate here (unlike T-7, phase 4f).
                    ? new InHomeStraight(0)
                    : new OnTrack(topology.step(index, direction));
            case InHomeStraight(int cell) -> cell == BoardTopology.HOME_STRAIGHT_LENGTH - 1
                    ? new AtHome()
                    : new InHomeStraight(cell + 1);
            case AtHome ignored -> throw new IllegalStateException("Cannot step further once at Home");
            case InBase ignored -> throw new IllegalArgumentException(
                    "MovementCalculator does not walk from base; use EnterFromBase for Rule 2");
        };
    }
}
