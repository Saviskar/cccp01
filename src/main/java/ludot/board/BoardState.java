package ludot.board;

import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.PieceEffect;
import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Where every piece is: the piece registry plus O(1) reverse lookups by track
 * and home-straight cell. Blocks are derived from occupancy, never stored
 * (A-14). Base and home occupancy are likewise derived from each piece's own
 * Position, not duplicated here.
 */
public final class BoardState implements GameView {

    private static final int PIECES_PER_COLOUR = 4;

    private final Map<PieceId, Piece> pieces = new LinkedHashMap<>();
    private final List<List<PieceId>> track = new ArrayList<>(BoardTopology.TRACK_SIZE);
    private final Map<Colour, List<List<PieceId>>> homeStraight = new EnumMap<>(Colour.class);

    public BoardState() {
        for (int i = 0; i < BoardTopology.TRACK_SIZE; i++) {
            track.add(new ArrayList<>());
        }
        for (Colour colour : Colour.values()) {
            List<List<PieceId>> cells = new ArrayList<>(BoardTopology.HOME_STRAIGHT_LENGTH);
            for (int i = 0; i < BoardTopology.HOME_STRAIGHT_LENGTH; i++) {
                cells.add(new ArrayList<>());
            }
            homeStraight.put(colour, cells);

            for (int number = 1; number <= PIECES_PER_COLOUR; number++) {
                PieceId id = new PieceId(colour, number);
                pieces.put(id, new Piece(id));
            }
        }
    }

    /**
     * Returns the live instance for {@code id}, for querying. {@link Piece}'s
     * mutators are package-private, so the compiler prevents callers outside
     * {@code ludot.board} from mutating it directly and desyncing occupancy
     * — they must go through this class's own mutation methods instead.
     */
    public Piece piece(PieceId id) {
        return pieces.get(id);
    }

    /** See {@link #piece(PieceId)}: the returned instances are for querying only. */
    public List<Piece> piecesOfColour(Colour colour) {
        List<Piece> result = new ArrayList<>(PIECES_PER_COLOUR);
        for (int number = 1; number <= PIECES_PER_COLOUR; number++) {
            result.add(pieces.get(new PieceId(colour, number)));
        }
        return result;
    }

    public List<PieceId> piecesAt(int trackIndex) {
        return List.copyOf(track.get(trackIndex));
    }

    public List<PieceId> piecesInHomeStraight(Colour colour, int cell) {
        return List.copyOf(homeStraight.get(colour).get(cell));
    }

    // A-14: a block is 2+ same-colour pieces sharing a standard track cell.
    public boolean isBlock(int trackIndex) {
        return track.get(trackIndex).size() >= 2;
    }

    public Optional<Colour> colourAt(int trackIndex) {
        List<PieceId> occupants = track.get(trackIndex);
        return occupants.isEmpty() ? Optional.empty() : Optional.of(occupants.get(0).colour());
    }

    public int countInBase(Colour colour) {
        return countWhere(colour, position -> position instanceof InBase);
    }

    public int countOnBoard(Colour colour) {
        return countWhere(colour, position -> position instanceof OnTrack || position instanceof InHomeStraight);
    }

    public int countAtHome(Colour colour) {
        return countWhere(colour, position -> position instanceof AtHome);
    }

    private int countWhere(Colour colour, Predicate<Position> predicate) {
        int count = 0;
        for (Piece piece : piecesOfColour(colour)) {
            if (predicate.test(piece.position())) {
                count++;
            }
        }
        return count;
    }

    public void moveTo(PieceId id, Position destination) {
        Piece piece = pieces.get(id);
        removeFromOccupancy(id, piece.position());
        piece.moveTo(destination);
        addToOccupancy(id, destination);
    }

    // T-9/A-26: clears the piece's old occupancy entry before resetting its fields,
    // so a return to base never leaves a stale track/home-straight entry behind.
    public void resetToBase(PieceId id) {
        Piece piece = pieces.get(id);
        removeFromOccupancy(id, piece.position());
        piece.resetToBase();
    }

    // A-07/T-7: eligibility for the home straight requires at least one capture.
    public void recordCapture(PieceId id) {
        pieces.get(id).recordCapture();
    }

    // A-12: set once by the base-to-X coin toss; T-14 (Gamma) later overwrites it permanently.
    public void assignDirection(PieceId id, Direction direction) {
        pieces.get(id).assignDirection(direction);
    }

    // A-08: counts crossings of the Approach cell while moving counterclockwise.
    public void recordApproachCrossing(PieceId id) {
        pieces.get(id).recordApproachCrossing();
    }

    // A-45: a new Alpha or Beta effect replaces any existing one.
    public void applyEffect(PieceId id, PieceEffect effect) {
        pieces.get(id).applyEffect(effect);
    }

    private void removeFromOccupancy(PieceId id, Position position) {
        switch (position) {
            case InBase() -> { }
            case OnTrack(int index) -> track.get(index).remove(id);
            case InHomeStraight(int cell) -> homeStraight.get(id.colour()).get(cell).remove(id);
            case AtHome() -> { }
        }
    }

    private void addToOccupancy(PieceId id, Position position) {
        switch (position) {
            case InBase() -> { }
            case OnTrack(int index) -> track.get(index).add(id);
            case InHomeStraight(int cell) -> homeStraight.get(id.colour()).get(cell).add(id);
            case AtHome() -> { }
        }
    }
}
