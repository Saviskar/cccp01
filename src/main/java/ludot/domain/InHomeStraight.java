package ludot.domain;

/** A piece on one of its colour's 5 home-straight cells (A-05). */
public record InHomeStraight(int cell) implements Position {

    // A-05: each home straight has 5 cells. Duplicated from the rules layer's
    // HOME_STRAIGHT_LENGTH because `domain` must not depend on it (DESIGN.md 2.2).
    private static final int HOME_STRAIGHT_LENGTH = 5;

    public InHomeStraight {
        if (cell < 0 || cell >= HOME_STRAIGHT_LENGTH) {
            throw new IllegalArgumentException("Home straight cell must be 0-4, was " + cell);
        }
    }
}
