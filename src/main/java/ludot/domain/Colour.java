package ludot.domain;

/**
 * A player's colour. The index is the multiplier {@code BoardTopology} uses
 * to compute each colour's X and Approach cells (A-02).
 */
public enum Colour {
    YELLOW(0),
    BLUE(1),
    RED(2),
    GREEN(3);

    private final int index;

    Colour(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
