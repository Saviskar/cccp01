package ludot.board;

import ludot.domain.Colour;
import ludot.domain.Direction;

/**
 * Pure geometry of the standard track: cell indices and stepping. Holds no
 * game state (A-14: even blocks are derived elsewhere from occupancy, not
 * from anything stored here).
 */
public final class BoardTopology {

    /** Number of cells on the standard track (A-01). */
    public static final int TRACK_SIZE = 52;

    /** Number of cells in each colour's home straight (A-05). */
    public static final int HOME_STRAIGHT_LENGTH = 5;

    // A-02: X offset = 13 x colourIndex.
    private static final int COLOUR_X_STEP = 13;

    // A-02: Approach is 2 cells before X.
    private static final int APPROACH_BEFORE_X = 2;

    // A-03: Alpha, Beta and Gamma counted clockwise from the Yellow Approach cell.
    private static final int YELLOW_APPROACH_TO_ALPHA = 9;
    private static final int YELLOW_APPROACH_TO_BETA = 27;
    private static final int YELLOW_APPROACH_TO_GAMMA = 46;

    public int xIndex(Colour colour) {
        return Math.floorMod(COLOUR_X_STEP * colour.index(), TRACK_SIZE);
    }

    public int approachIndex(Colour colour) {
        return Math.floorMod(xIndex(colour) - APPROACH_BEFORE_X, TRACK_SIZE);
    }

    public int alphaIndex() {
        return specialCellIndex(YELLOW_APPROACH_TO_ALPHA);
    }

    public int betaIndex() {
        return specialCellIndex(YELLOW_APPROACH_TO_BETA);
    }

    public int gammaIndex() {
        return specialCellIndex(YELLOW_APPROACH_TO_GAMMA);
    }

    private int specialCellIndex(int offsetFromYellowApproach) {
        return Math.floorMod(approachIndex(Colour.YELLOW) + offsetFromYellowApproach, TRACK_SIZE);
    }

    public int step(int trackIndex, Direction direction) {
        int delta = direction == Direction.CLOCKWISE ? 1 : -1;
        return Math.floorMod(trackIndex + delta, TRACK_SIZE);
    }
}
