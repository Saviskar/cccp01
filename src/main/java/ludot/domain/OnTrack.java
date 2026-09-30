package ludot.domain;

/** A piece on the 52-cell standard track (A-01). */
public record OnTrack(int index) implements Position {

    // A-01: the standard track has 52 cells. Duplicated from BoardTopology.TRACK_SIZE
    // because `domain` must not depend on `board` (DESIGN.md 2.2).
    private static final int TRACK_SIZE = 52;

    public OnTrack {
        if (index < 0 || index >= TRACK_SIZE) {
            throw new IllegalArgumentException("Track index must be 0-51, was " + index);
        }
    }
}
