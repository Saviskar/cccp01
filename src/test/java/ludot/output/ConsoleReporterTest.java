package ludot.output;

import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.BriefingAssigned;
import ludot.events.GammaRerouteTriggered;
import ludot.events.MysteryCellTriggered;
import ludot.events.PieceBlocked;
import ludot.events.PieceCaptured;
import ludot.events.PieceCountStatus;
import ludot.events.PieceEnteredX;
import ludot.events.PieceMoved;
import ludot.events.PiecePartiallyMoved;
import ludot.events.PieceTeleported;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleReporterTest {

    private static final PieceId R1 = new PieceId(Colour.RED, 1);
    private static final PieceId G2 = new PieceId(Colour.GREEN, 2);
    private static final String NL = System.lineSeparator();

    private ByteArrayOutputStream captured;
    private ConsoleReporter reporter;

    @BeforeEach
    void setUp() {
        captured = new ByteArrayOutputStream();
        reporter = new ConsoleReporter(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("a single-line event prints exactly one line, with the platform line separator")
    void singleLineEventPrintsOneLine() {
        reporter.onEvent(new PieceCountStatus(Colour.RED, 2, 2));

        assertEquals(
                "Red player now has 2/4 on pieces on the board and 2/4 pieces on the base." + NL,
                output());
    }

    @Test
    @DisplayName("a multi-line event (PieceEnteredX) prints each line separately via println")
    void multiLineEventPrintsEachLineSeparately() {
        reporter.onEvent(new PieceEnteredX(R1, 1, 3));

        assertEquals(
                "Red player moves piece R1 to the starting point." + NL
                        + "Red player now has 1/4 on pieces on the board and 3/4 pieces on the base." + NL,
                output());
    }

    @Test
    @DisplayName("A-46: a capturing move prints the movement fact before the capture fact")
    void a46_moveThenCaptureOrder() {
        reporter.onEvent(new PieceMoved(R1, new OnTrack(5), new OnTrack(11), 6, Direction.CLOCKWISE));
        reporter.onEvent(new PieceCaptured(R1, new OnTrack(11), G2));
        reporter.onEvent(new PieceCountStatus(Colour.GREEN, 3, 1));

        assertEquals(
                "Red moves piece R1 from location 5 to 11 by 6 units in clockwise direction." + NL
                        + "Red piece 1 lands on square 11, captures green piece 2, and returns it to the base." + NL
                        + "Green player now has 3/4 on pieces on the board and 1/4 pieces on the base." + NL,
                output());
    }

    @Test
    @DisplayName("A-48: an obstructed partial move prints the blocked fact before the partial-move fact")
    void a48_blockedThenPartialMoveOrder() {
        reporter.onEvent(new PieceBlocked(R1, new OnTrack(0), new OnTrack(6), G2));
        reporter.onEvent(new PiecePartiallyMoved(R1, new OnTrack(0), new OnTrack(3), 3, Direction.CLOCKWISE));

        assertEquals(
                "Red piece 1 is blocked from moving from 0 to 6 by green piece 2." + NL
                        + "Red does not have other pieces in the board to move instead of the blocked piece. "
                        + "Moved the piece to square 3 which is the cell before the block." + NL,
                output());
    }

    @Test
    @DisplayName("A-60: the generic mystery line, the redirect explanation, then the Base line, in order")
    void a60_redirectSequence() {
        reporter.onEvent(new MysteryCellTriggered(R1, MysteryOutcomeKind.ALPHA));
        reporter.onEvent(new PieceTeleported(R1, MysteryOutcomeKind.ALPHA, new InBase(), true, Optional.of(Colour.GREEN)));

        assertEquals(
                "Red player lands on a mystery cell and is teleported to Alpha." + NL
                        + "Alpha is occupied by a green blockade, so red piece 1 is sent to base instead." + NL
                        + "Red piece 1 teleported to Base." + NL,
                output());
    }

    @Test
    @DisplayName("A-58: a Gamma reroute prints a second teleport fact, then the Beta-side effect fact")
    void a58_gammaRerouteDoubleTeleport() {
        reporter.onEvent(new MysteryCellTriggered(R1, MysteryOutcomeKind.GAMMA));
        reporter.onEvent(new PieceTeleported(R1, MysteryOutcomeKind.GAMMA, new OnTrack(44), false, Optional.empty()));
        reporter.onEvent(new GammaRerouteTriggered(R1));
        reporter.onEvent(new PieceTeleported(R1, MysteryOutcomeKind.BETA, new OnTrack(25), false, Optional.empty()));
        reporter.onEvent(new BriefingAssigned(R1));

        assertEquals(
                "Red player lands on a mystery cell and is teleported to Gamma." + NL
                        + "Red piece 1 teleported to Gamma." + NL
                        + "The red piece 1 is moving in a counterclockwise direction. Teleporting to Beta from Gamma." + NL
                        + "Red piece 1 teleported to Beta." + NL
                        + "Red piece 1 attends briefing and cannot move for four rounds." + NL,
                output());
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }
}
