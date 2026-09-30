package ludot.domain;

/** Identifies one of a colour's four pieces (e.g. R1..R4). */
public record PieceId(Colour colour, int number) {

    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 4;

    public PieceId {
        if (number < MIN_NUMBER || number > MAX_NUMBER) {
            throw new IllegalArgumentException("Piece number must be 1-4, was " + number);
        }
    }
}
