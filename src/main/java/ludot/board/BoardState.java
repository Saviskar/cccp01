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
import ludot.random.RandomPicker;

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
    private final MysteryCell mysteryCell = new MysteryCell();
    // Pure geometry with no fields (A-01/A-02), so owning an instance here needs no constructor
    // parameter and changes no behaviour, unlike Dice/Coin/RandomPicker.
    private final BoardTopology topology = new BoardTopology();

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

    // A-14: all standard-track cells currently occupied by 2+ of this colour's own pieces. Derived
    // from the colour's own (at most 4) pieces, rather than scanning every track cell: a block can
    // only ever be at a cell one of this colour's pieces occupies.
    public List<Integer> blockCellsOf(Colour colour) {
        List<Integer> cells = new ArrayList<>();
        for (Piece piece : piecesOfColour(colour)) {
            if (piece.position() instanceof OnTrack(int idx) && isBlock(idx) && !cells.contains(idx)) {
                cells.add(idx);
            }
        }
        return cells;
    }

    // A-28: whether any piece currently occupies a standard-track cell, used to start the
    // mystery cell's spawn timer.
    public boolean anyPieceOnTrack() {
        return track.stream().anyMatch(occupants -> !occupants.isEmpty());
    }

    public Optional<Integer> mysteryCellLocation() {
        return mysteryCell.location();
    }

    // A-28: advances the mystery cell's timer by one round-end, picking a spawn/respawn
    // cell via the injected picker when one is due.
    public Optional<MysteryCellTick> tickMysteryCell(RandomPicker picker) {
        return mysteryCell.onRoundEnd(anyPieceOnTrack(), emptyTrackCells(), picker);
    }

    private List<Integer> emptyTrackCells() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < BoardTopology.TRACK_SIZE; i++) {
            if (track.get(i).isEmpty()) {
                empty.add(i);
            }
        }
        return empty;
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

    // A-13 (amended): shared by MoveGenerator's block-direction tie-break (A-17) and
    // BlockBreakPlanner's farthest-member tie-break (A-22), instead of each computing it itself.
    public int distanceFromHome(PieceId id) {
        Piece piece = pieces.get(id);
        return switch (piece.position()) {
            case OnTrack(int idx) -> topology.distanceFromHome(
                    id.colour(), piece.originalDirection().orElseThrow(), idx, piece.ccwApproachCrossings());
            case InHomeStraight(int cell) -> BoardTopology.HOME_STRAIGHT_LENGTH - cell;
            case AtHome ignored -> 0;
            case InBase ignored -> BoardTopology.IN_BASE_DISTANCE;
        };
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
