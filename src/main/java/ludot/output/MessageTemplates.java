package ludot.output;

import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.AlphaEffectAssigned;
import ludot.events.BlockMoved;
import ludot.events.BlockadeBroken;
import ludot.events.BriefingAssigned;
import ludot.events.BriefingStreakTriggered;
import ludot.events.DiceRolled;
import ludot.events.GameEnded;
import ludot.events.GammaDirectionReversed;
import ludot.events.GammaRerouteTriggered;
import ludot.events.MysteryCellStatusReported;
import ludot.events.MysteryCellTriggered;
import ludot.events.MysterySpawned;
import ludot.events.NoLegalMove;
import ludot.events.OpeningRollRolled;
import ludot.events.OpeningRollWinnerDetermined;
import ludot.events.PieceBlocked;
import ludot.events.PieceCaptured;
import ludot.events.PieceCountStatus;
import ludot.events.PieceDirectionAssigned;
import ludot.events.PieceEnteredX;
import ludot.events.PieceLocation;
import ludot.events.PieceMoved;
import ludot.events.PiecePartiallyMoved;
import ludot.events.PieceTeleported;
import ludot.events.PiecesIntroduced;
import ludot.events.PlayerFinished;
import ludot.events.RoundOrderAnnounced;
import ludot.events.RoundStatusReported;
import ludot.events.SeedSelected;
import ludot.events.ThirdSixIgnored;
import ludot.events.ThrowIgnoredAfterBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Formats every {@link ludot.events.GameEvent} into the exact Section 3 text (A-43), or clearly
 * worded additional text for the events the brief doesn't define a message for (A-44). Pure
 * string formatting only - no I/O (DESIGN.md 4.3: only {@link ConsoleReporter} prints). Every
 * method returns one element per physical line, so {@link ConsoleReporter} never has to special
 * case multi-line events and nothing here embeds a platform-specific line separator.
 */
final class MessageTemplates {

    // A-47: a six always grants a bonus roll, even when it produces no legal move.
    private static final int SIX = 6;
    private static final int PIECES_PER_COLOUR = 4;
    private static final int WINNING_PLACE = 1;
    private static final int FOURTH_PLACE = 4;

    private MessageTemplates() {
    }

    // A-72: wording and first-publish placement.
    static List<String> seedSelected(SeedSelected event) {
        return List.of(
                "This run uses seed " + event.seed() + ".",
                "Rerun with --seed " + event.seed() + " to reproduce this exact game.");
    }

    static List<String> piecesIntroduced(PiecesIntroduced event) {
        Colour colour = event.colour();
        String letter = colourLetter(colour);
        return List.of("The " + colourWord(colour, false) + " player has four (04) pieces named "
                + letter + "1, " + letter + "2, " + letter + "3, and " + letter + "4.");
    }

    static List<String> openingRollRolled(OpeningRollRolled event) {
        return List.of(colourWord(event.colour(), true) + " rolls " + event.value());
    }

    static List<String> openingRollWinnerDetermined(OpeningRollWinnerDetermined event) {
        return List.of(colourWord(event.colour(), true)
                + " player has the highest roll and will begin the game.");
    }

    static List<String> roundOrderAnnounced(RoundOrderAnnounced event) {
        String names = joinWithAnd(event.order().stream().map(colour -> colourWord(colour, false)).toList());
        return List.of("The order of a single round is " + names + ".");
    }

    static List<String> diceRolled(DiceRolled event) {
        return List.of(colourWord(event.colour(), true) + " player rolled " + event.value() + ".");
    }

    static List<String> pieceEnteredX(PieceEnteredX event) {
        PieceId id = event.pieceId();
        return List.of(
                colourWord(id.colour(), true) + " player moves piece " + fullPieceName(id) + " to the starting point.",
                pieceCountLine(id.colour(), event.onBoard(), event.inBase()));
    }

    static List<String> pieceDirectionAssigned(PieceDirectionAssigned event) {
        PieceId id = event.pieceId();
        return List.of(colourWord(id.colour(), true) + " piece " + id.number() + " tosses a coin and will move "
                + directionWord(event.direction()) + ".");
    }

    static List<String> pieceMoved(PieceMoved event) {
        PieceId id = event.pieceId();
        return List.of(colourWord(id.colour(), true) + " moves piece " + fullPieceName(id) + " from location "
                + squareName(event.from(), id.colour()) + " to " + squareName(event.to(), id.colour()) + " by "
                + event.units() + " units in " + directionWord(event.direction()) + " direction.");
    }

    static List<String> pieceCaptured(PieceCaptured event) {
        PieceId capturer = event.capturerId();
        PieceId captured = event.capturedId();
        return List.of(colourWord(capturer.colour(), true) + " piece " + capturer.number() + " lands on square "
                + squareName(event.landedOn(), capturer.colour()) + ", captures "
                + colourWord(captured.colour(), false) + " piece " + captured.number() + ", and returns it to the base.");
    }

    static List<String> pieceCountStatus(PieceCountStatus event) {
        return List.of(pieceCountLine(event.colour(), event.onBoard(), event.inBase()));
    }

    static List<String> pieceBlocked(PieceBlocked event) {
        PieceId id = event.pieceId();
        PieceId blocker = event.blockingPieceId();
        return List.of(colourWord(id.colour(), true) + " piece " + id.number() + " is blocked from moving from "
                + squareName(event.from(), id.colour()) + " to " + squareName(event.intendedDestination(), id.colour())
                + " by " + colourWord(blocker.colour(), false) + " piece " + blocker.number() + ".");
    }

    static List<String> throwIgnoredAfterBlock(ThrowIgnoredAfterBlock event) {
        return List.of(colourWord(event.colour(), true) + " does not have other pieces in the board to move instead "
                + "of the blocked piece. Ignoring the throw and moving on to the next player.");
    }

    static List<String> piecePartiallyMoved(PiecePartiallyMoved event) {
        PieceId id = event.pieceId();
        return List.of(colourWord(id.colour(), true) + " does not have other pieces in the board to move instead "
                + "of the blocked piece. Moved the piece to square " + squareName(event.to(), id.colour())
                + " which is the cell before the block.");
    }

    // A-47: a six still grants its bonus roll even when nothing could legally move.
    static List<String> noLegalMove(NoLegalMove event) {
        String colour = colourWord(event.colour(), true);
        String ending = event.rollValue() == SIX ? "rolls again." : "passes the dice.";
        return List.of(colour + " player has no legal move with a roll of " + event.rollValue() + " and " + ending);
    }

    static List<String> thirdSixIgnored(ThirdSixIgnored event) {
        return List.of(colourWord(event.colour(), true)
                + " player rolled a third consecutive six; the roll is ignored and the dice passes to the next player.");
    }

    static List<String> blockMoved(BlockMoved event) {
        Colour colour = event.pieceIds().get(0).colour();
        String names = joinWithAnd(event.pieceIds().stream().map(MessageTemplates::fullPieceName).toList());
        return List.of(colourWord(colour, true) + " moves its block of " + event.pieceIds().size() + " pieces ("
                + names + ") from " + squareName(event.from(), colour) + " to " + squareName(event.to(), colour)
                + " by " + event.cellsPerPiece() + " units each in " + directionWord(event.direction())
                + " direction (rolled " + event.rollValue() + ").");
    }

    static List<String> blockadeBroken(BlockadeBroken event) {
        String staying = joinWithAnd(event.staying().stream().map(MessageTemplates::fullPieceName).toList());
        String leaving = joinWithAnd(event.leaving().stream().map(MessageTemplates::fullPieceName).toList());
        return List.of(colourWord(event.colour(), true) + " blockade at square " + event.cell() + " breaks: "
                + staying + " stay, " + leaving + " leave, each moving " + event.unitsEach()
                + " units in its own original direction.");
    }

    static List<String> roundStatusReported(RoundStatusReported event) {
        List<String> lines = new ArrayList<>();
        lines.add(pieceCountLine(event.colour(), event.onBoard(), event.inBase()));
        lines.add("============================");
        lines.add("Location of pieces " + colourWord(event.colour(), false));
        lines.add("============================");
        for (PieceLocation location : event.locations()) {
            lines.add("Piece " + location.pieceId().number() + " -> " + squareName(location.position(), event.colour()) + ".");
        }
        return List.copyOf(lines);
    }

    static List<String> mysterySpawned(MysterySpawned event) {
        return List.of("A mystery cell has spawned in location " + event.location()
                + " and will be at this location for the next four rounds.");
    }

    static List<String> mysteryCellStatusReported(MysteryCellStatusReported event) {
        return List.of("The mystery cell is at " + event.location() + " and will be at that location for the next "
                + event.roundsRemaining() + " values.");
    }

    static List<String> mysteryCellTriggered(MysteryCellTriggered event) {
        PieceId id = event.pieceId();
        return List.of(colourWord(id.colour(), true) + " player lands on a mystery cell and is teleported to "
                + kindWord(event.destination()) + ".");
    }

    // A-60: a redirect (opponent block on the drawn cell) gets an extra explanatory line, then
    // reuses the literal Base template to reflect where the piece actually ended up.
    static List<String> pieceTeleported(PieceTeleported event) {
        PieceId id = event.pieceId();
        String kind = kindWord(event.destination());
        if (!event.redirectedToBase()) {
            return List.of(colourWord(id.colour(), true) + " piece " + id.number() + " teleported to " + kind + ".");
        }
        Colour blocker = event.blockingColour().orElseThrow();
        return List.of(
                kind + " is occupied by a " + colourWord(blocker, false) + " blockade, so "
                        + colourWord(id.colour(), false) + " piece " + id.number() + " is sent to base instead.",
                colourWord(id.colour(), true) + " piece " + id.number() + " teleported to Base.");
    }

    static List<String> alphaEffectAssigned(AlphaEffectAssigned event) {
        PieceId id = event.pieceId();
        String effect = switch (event.kind()) {
            case ENERGISED -> "feels energized, and movement speed doubles.";
            case SICK -> "feels sick, and movement speed halves.";
        };
        return List.of(colourWord(id.colour(), true) + " piece " + id.number() + " " + effect);
    }

    static List<String> briefingAssigned(BriefingAssigned event) {
        PieceId id = event.pieceId();
        return List.of(colourWord(id.colour(), true) + " piece " + id.number()
                + " attends briefing and cannot move for four rounds.");
    }

    static List<String> briefingStreakTriggered(BriefingStreakTriggered event) {
        PieceId id = event.pieceId();
        return List.of(colourWord(id.colour(), true) + " piece " + id.number()
                + " is movement-restricted and has rolled three consecutively. Teleporting piece " + id.number()
                + " to base.");
    }

    static List<String> gammaDirectionReversed(GammaDirectionReversed event) {
        PieceId id = event.pieceId();
        return List.of("The " + colourWord(id.colour(), false) + " piece " + id.number()
                + ", which was moving clockwise, has changed to moving counterclockwise.");
    }

    static List<String> gammaRerouteTriggered(GammaRerouteTriggered event) {
        PieceId id = event.pieceId();
        return List.of("The " + colourWord(id.colour(), false) + " piece " + id.number()
                + " is moving in a counterclockwise direction. Teleporting to Beta from Gamma.");
    }

    // A-41/A-61: every colour that actually finishes (places 1-3) reports its own live line —
    // place 1's win, or an ordinary placing line otherwise. Only the auto-assigned 4th place
    // (A-41) never finishes live, so it has no PlayerFinished event and is reported by GameEnded.
    static List<String> playerFinished(PlayerFinished event) {
        if (event.place() == WINNING_PLACE) {
            return List.of(colourWord(event.colour(), true) + " player wins!!!");
        }
        return List.of(placingLine(event.colour(), event.place()));
    }

    static List<String> gameEnded(GameEnded event) {
        if (event.stoppedByRoundGuard()) {
            return List.of(roundGuardSummaryLine(event));
        }
        // A-41/A-61: places 1-3 already announced their own live PlayerFinished message; only
        // the auto-assigned 4th place has never been announced.
        Colour fourthPlace = event.placings().get(FOURTH_PLACE - 1);
        return List.of(placingLine(fourthPlace, FOURTH_PLACE));
    }

    // A-61: the round guard never re-announces a placing (every finisher already got its live
    // PlayerFinished message); one summary line instead names finishers, in finishing order, and
    // lists who never finished, in the fixed turn order (A-04).
    private static String roundGuardSummaryLine(GameEnded event) {
        List<Colour> finishers = event.placings();
        String finishedPart = finishers.isEmpty() ? "none" : String.join(", ", IntStream.range(0, finishers.size())
                .mapToObj(i -> ordinal(i + 1) + " " + colourWord(finishers.get(i), false))
                .toList());
        String notFinishedPart = String.join(", ",
                event.notFinished().stream().map(colour -> colourWord(colour, false)).toList());
        return "The game reached the 1000-round limit. Finished: " + finishedPart + ". Did not finish: "
                + notFinishedPart + ".";
    }

    private static String placingLine(Colour colour, int place) {
        return colourWord(colour, true) + " player finishes in " + ordinal(place) + " place.";
    }

    private static String pieceCountLine(Colour colour, int onBoard, int inBase) {
        return colourWord(colour, true) + " player now has " + onBoard + "/" + PIECES_PER_COLOUR
                + " on pieces on the board and " + inBase + "/" + PIECES_PER_COLOUR + " pieces on the base.";
    }

    private static String fullPieceName(PieceId id) {
        return colourLetter(id.colour()) + id.number();
    }

    private static String directionWord(Direction direction) {
        return direction == Direction.CLOCKWISE ? "clockwise" : "counterclockwise";
    }

    private static String kindWord(MysteryOutcomeKind kind) {
        return switch (kind) {
            case ALPHA -> "Alpha";
            case BETA -> "Beta";
            case GAMMA -> "Gamma";
            case BASE -> "Base";
            case X -> "X";
            case APPROACH -> "Approach";
        };
    }

    private static String ordinal(int place) {
        return switch (place) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> place + "th";
        };
    }

    private static String colourWord(Colour colour, boolean capitalised) {
        String lower = colour.name().toLowerCase();
        return capitalised ? Character.toUpperCase(lower.charAt(0)) + lower.substring(1) : lower;
    }

    // A-02: the colour-letter + number naming from the brief's own §1.1 example (R1, G1, ...).
    private static String colourLetter(Colour colour) {
        return switch (colour) {
            case YELLOW -> "Y";
            case BLUE -> "B";
            case RED -> "R";
            case GREEN -> "G";
        };
    }

    // §3 legend: standard-track cells are bare indices; home-straight cells are
    // "<colour>homepath<n>"; base and home are the literal words "Base"/"Home".
    private static String squareName(Position position, Colour owner) {
        return switch (position) {
            case OnTrack(int index) -> Integer.toString(index);
            case InHomeStraight(int cell) -> colourWord(owner, false) + "homepath" + cell;
            case InBase ignored -> "Base";
            case AtHome ignored -> "Home";
        };
    }

    // "a" / "a and b" / "a, b, and c" - the brief's own list style (RoundOrderAnnounced, §3).
    private static String joinWithAnd(List<String> items) {
        int size = items.size();
        if (size == 1) {
            return items.get(0);
        }
        if (size == 2) {
            return items.get(0) + " and " + items.get(1);
        }
        String allButLast = String.join(", ", items.subList(0, size - 1));
        return allButLast + ", and " + items.get(size - 1);
    }
}
