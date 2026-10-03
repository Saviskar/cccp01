package ludot.output;

import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.BlockMoved;
import ludot.events.BlockadeBroken;
import ludot.events.DiceRolled;
import ludot.events.GameEvent;
import ludot.events.MysteryCellStatusReported;
import ludot.events.MysteryCellTriggered;
import ludot.events.OpeningRollRolled;
import ludot.events.OpeningRollWinnerDetermined;
import ludot.events.PieceBlocked;
import ludot.events.PieceCaptured;
import ludot.events.PieceCountStatus;
import ludot.events.PieceEnteredX;
import ludot.events.PieceLocation;
import ludot.events.PieceMoved;
import ludot.events.PiecePartiallyMoved;
import ludot.events.PieceTeleported;
import ludot.events.PiecesIntroduced;
import ludot.events.PlayerFinished;
import ludot.events.RoundOrderAnnounced;
import ludot.events.RoundStatusReported;
import ludot.events.ThirdSixIgnored;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DESIGN.md §10's golden-file test for phase 5: a single scripted slice of a game, run through
 * one {@link ConsoleReporter}, checked against one expected transcript. The event data below
 * isn't a real simulated game (the rule engine isn't invoked) - it's hand-built to exercise one
 * representative instance of every message family in one ordered pass, including the orderings
 * the rules already fix (A-46, A-48, A-60). A full seeded end-to-end run is phase 7's job, once
 * {@code Main}/{@code GameFactory} exist.
 */
class ConsoleReporterGoldenTest {

    private static final String NL = System.lineSeparator();

    @Test
    @DisplayName("golden transcript: one representative slice of a game")
    void goldenTranscript() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ConsoleReporter reporter = new ConsoleReporter(new PrintStream(captured, true, StandardCharsets.UTF_8));

        List<PieceId> red = pieces(Colour.RED);
        List<PieceId> green = pieces(Colour.GREEN);
        List<PieceId> yellow = pieces(Colour.YELLOW);
        List<PieceId> blue = pieces(Colour.BLUE);
        PieceId g1 = green.get(0);
        PieceId r1 = red.get(0);
        PieceId y1 = yellow.get(0);
        PieceId b1 = blue.get(0);

        List<GameEvent> script = List.of(
                new PiecesIntroduced(Colour.RED, red),
                new PiecesIntroduced(Colour.GREEN, green),
                new PiecesIntroduced(Colour.YELLOW, yellow),
                new PiecesIntroduced(Colour.BLUE, blue),
                new OpeningRollRolled(Colour.RED, 4),
                new OpeningRollRolled(Colour.GREEN, 6),
                new OpeningRollRolled(Colour.YELLOW, 3),
                new OpeningRollRolled(Colour.BLUE, 2),
                new OpeningRollWinnerDetermined(Colour.GREEN),
                new RoundOrderAnnounced(List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE, Colour.RED)),
                // base entry, no capture
                new DiceRolled(Colour.GREEN, 6),
                new PieceEnteredX(g1, 1, 3),
                // ordinary move that captures (A-46: movement fact, then capture fact)
                new DiceRolled(Colour.GREEN, 5),
                new PieceMoved(g1, new OnTrack(39), new OnTrack(44), 5, Direction.CLOCKWISE),
                new PieceCaptured(g1, new OnTrack(44), r1),
                new PieceCountStatus(Colour.RED, 3, 1),
                // obstructed partial move (A-16/A-48: blocked fact, then partial-move fact)
                new DiceRolled(Colour.YELLOW, 4),
                new PieceBlocked(y1, new OnTrack(0), new OnTrack(4), b1),
                new PiecePartiallyMoved(y1, new OnTrack(0), new OnTrack(3), 3, Direction.CLOCKWISE),
                // a block move (A-44)
                new DiceRolled(Colour.BLUE, 6),
                new BlockMoved(List.of(blue.get(0), blue.get(1)), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE),
                // three consecutive sixes break a block (T-6/A-22)
                new DiceRolled(Colour.RED, 6),
                new ThirdSixIgnored(Colour.RED),
                new BlockadeBroken(Colour.RED, 20, List.of(red.get(1)), List.of(red.get(2), red.get(3)), 3),
                // a mystery teleport redirected to base by an opponent block (A-31/A-60)
                new DiceRolled(Colour.GREEN, 3),
                new MysteryCellTriggered(g1, MysteryOutcomeKind.ALPHA),
                new PieceTeleported(g1, MysteryOutcomeKind.ALPHA, new InBase(), true, Optional.of(Colour.BLUE)),
                // round status, then mystery status
                new RoundStatusReported(Colour.GREEN, 0, 4, List.of(
                        new PieceLocation(green.get(0), new InBase()),
                        new PieceLocation(green.get(1), new InBase()),
                        new PieceLocation(green.get(2), new InBase()),
                        new PieceLocation(green.get(3), new InBase()))),
                new MysteryCellStatusReported(30, 4),
                // a win
                new PlayerFinished(Colour.YELLOW, 1));

        script.forEach(reporter::onEvent);

        String expected = String.join(NL, List.of(
                "The red player has four (04) pieces named R1, R2, R3, and R4.",
                "The green player has four (04) pieces named G1, G2, G3, and G4.",
                "The yellow player has four (04) pieces named Y1, Y2, Y3, and Y4.",
                "The blue player has four (04) pieces named B1, B2, B3, and B4.",
                "Red rolls 4",
                "Green rolls 6",
                "Yellow rolls 3",
                "Blue rolls 2",
                "Green player has the highest roll and will begin the game.",
                "The order of a single round is green, yellow, blue, and red.",
                "Green player rolled 6.",
                "Green player moves piece G1 to the starting point.",
                "Green player now has 1/4 on pieces on the board and 3/4 pieces on the base.",
                "Green player rolled 5.",
                "Green moves piece G1 from location 39 to 44 by 5 units in clockwise direction.",
                "Green piece 1 lands on square 44, captures red piece 1, and returns it to the base.",
                "Red player now has 3/4 on pieces on the board and 1/4 pieces on the base.",
                "Yellow player rolled 4.",
                "Yellow piece 1 is blocked from moving from 0 to 4 by blue piece 1.",
                "Yellow does not have other pieces in the board to move instead of the blocked piece. "
                        + "Moved the piece to square 3 which is the cell before the block.",
                "Blue player rolled 6.",
                "Blue moves its block of 2 pieces (B1 and B2) from 10 to 13 by 3 units each in "
                        + "clockwise direction (rolled 6).",
                "Red player rolled 6.",
                "Red player rolled a third consecutive six; the roll is ignored and the dice passes "
                        + "to the next player.",
                "Red blockade at square 20 breaks: R2 stay, R3 and R4 leave, each moving 3 units in "
                        + "its own original direction.",
                "Green player rolled 3.",
                "Green player lands on a mystery cell and is teleported to Alpha.",
                "Alpha is occupied by a blue blockade, so green piece 1 is sent to base instead.",
                "Green piece 1 teleported to Base.",
                "Green player now has 0/4 on pieces on the board and 4/4 pieces on the base.",
                "============================",
                "Location of pieces green",
                "============================",
                "Piece 1 -> Base.",
                "Piece 2 -> Base.",
                "Piece 3 -> Base.",
                "Piece 4 -> Base.",
                "The mystery cell is at 30 and will be at that location for the next 4 values.",
                "Yellow player wins!!!")) + NL;

        assertEquals(expected, captured.toString(StandardCharsets.UTF_8));
    }

    private static List<PieceId> pieces(Colour colour) {
        return List.of(
                new PieceId(colour, 1), new PieceId(colour, 2), new PieceId(colour, 3), new PieceId(colour, 4));
    }
}
