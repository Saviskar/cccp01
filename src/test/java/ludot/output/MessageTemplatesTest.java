package ludot.output;

import ludot.domain.AlphaEffectKind;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
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
import ludot.events.ThirdSixIgnored;
import ludot.events.ThrowIgnoredAfterBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTemplatesTest {

    private static final PieceId R1 = new PieceId(Colour.RED, 1);
    private static final PieceId G2 = new PieceId(Colour.GREEN, 2);

    @Test
    @DisplayName("A-43: piecesIntroduced: verbatim intro line, §3")
    void piecesIntroduced() {
        List<PieceId> ids = List.of(
                new PieceId(Colour.RED, 1), new PieceId(Colour.RED, 2),
                new PieceId(Colour.RED, 3), new PieceId(Colour.RED, 4));

        assertEquals(
                List.of("The red player has four (04) pieces named R1, R2, R3, and R4."),
                MessageTemplates.piecesIntroduced(new PiecesIntroduced(Colour.RED, ids)));
    }

    @Test
    @DisplayName("A-43: openingRollRolled: colour capitalised at sentence start, §3")
    void openingRollRolled() {
        assertEquals(
                List.of("Red rolls 4"),
                MessageTemplates.openingRollRolled(new OpeningRollRolled(Colour.RED, 4)));
    }

    @Test
    @DisplayName("A-43: openingRollWinnerDetermined: verbatim, §3")
    void openingRollWinnerDetermined() {
        assertEquals(
                List.of("Green player has the highest roll and will begin the game."),
                MessageTemplates.openingRollWinnerDetermined(new OpeningRollWinnerDetermined(Colour.GREEN)));
    }

    @Test
    @DisplayName("A-43: roundOrderAnnounced: lowercase mid-sentence, Oxford comma before 'and', §3")
    void roundOrderAnnounced() {
        assertEquals(
                List.of("The order of a single round is red, green, yellow, and blue."),
                MessageTemplates.roundOrderAnnounced(new RoundOrderAnnounced(
                        List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE))));
    }

    @Test
    @DisplayName("A-43: diceRolled: verbatim, §3")
    void diceRolled() {
        assertEquals(
                List.of("Red player rolled 5."),
                MessageTemplates.diceRolled(new DiceRolled(Colour.RED, 5)));
    }

    @Test
    @DisplayName("a59: base-to-X message names the piece as colour-letter+number (R1), not literal X")
    void a59_pieceEnteredXUsesFullPieceName() {
        assertEquals(
                List.of(
                        "Red player moves piece R1 to the starting point.",
                        "Red player now has 1/4 on pieces on the board and 3/4 pieces on the base."),
                MessageTemplates.pieceEnteredX(new PieceEnteredX(R1, 1, 3)));
    }

    @Test
    @DisplayName("pieceDirectionAssigned: A-44 coin-toss wording")
    void pieceDirectionAssigned() {
        assertEquals(
                List.of("Red piece 1 tosses a coin and will move clockwise."),
                MessageTemplates.pieceDirectionAssigned(new PieceDirectionAssigned(R1, Direction.CLOCKWISE)));
    }

    @Test
    @DisplayName("a59: ordinary move message names the piece as colour-letter+number (R1), not literal X")
    void a59_pieceMovedUsesFullPieceName() {
        assertEquals(
                List.of("Red moves piece R1 from location 5 to 11 by 6 units in clockwise direction."),
                MessageTemplates.pieceMoved(new PieceMoved(R1, new OnTrack(5), new OnTrack(11), 6, Direction.CLOCKWISE)));
    }

    @Test
    @DisplayName("A-43: pieceCaptured: capturer capitalised, captured colour lowercase mid-sentence, §3")
    void pieceCaptured() {
        assertEquals(
                List.of("Red piece 1 lands on square 11, captures green piece 2, and returns it to the base."),
                MessageTemplates.pieceCaptured(new PieceCaptured(R1, new OnTrack(11), G2)));
    }

    @Test
    @DisplayName("A-46: the count line after a capture reports the captured colour, capitalised")
    void a46_pieceCountStatusReportsCapturedColour() {
        assertEquals(
                List.of("Green player now has 3/4 on pieces on the board and 1/4 pieces on the base."),
                MessageTemplates.pieceCountStatus(new PieceCountStatus(Colour.GREEN, 3, 1)));
    }

    @Test
    @DisplayName("A-43: pieceBlocked: mover capitalised, blocking colour lowercase mid-sentence, §3")
    void pieceBlocked() {
        assertEquals(
                List.of("Red piece 1 is blocked from moving from 5 to 11 by green piece 2."),
                MessageTemplates.pieceBlocked(new PieceBlocked(R1, new OnTrack(5), new OnTrack(11), G2)));
    }

    @Test
    @DisplayName("throwIgnoredAfterBlock: verbatim, §3/A-48")
    void throwIgnoredAfterBlock() {
        assertEquals(
                List.of("Red does not have other pieces in the board to move instead of the blocked piece. "
                        + "Ignoring the throw and moving on to the next player."),
                MessageTemplates.throwIgnoredAfterBlock(new ThrowIgnoredAfterBlock(Colour.RED)));
    }

    @Test
    @DisplayName("piecePartiallyMoved: verbatim, §3/A-16")
    void piecePartiallyMoved() {
        assertEquals(
                List.of("Red does not have other pieces in the board to move instead of the blocked piece. "
                        + "Moved the piece to square 3 which is the cell before the block."),
                MessageTemplates.piecePartiallyMoved(
                        new PiecePartiallyMoved(R1, new OnTrack(0), new OnTrack(3), 3, Direction.CLOCKWISE)));
    }

    @Test
    @DisplayName("A-44/A-47: a roll of six with no legal move still rolls again")
    void a47_noLegalMoveOnSixStillRollsAgain() {
        assertEquals(
                List.of("Red player has no legal move with a roll of 6 and rolls again."),
                MessageTemplates.noLegalMove(new NoLegalMove(Colour.RED, 6)));
    }

    @Test
    @DisplayName("A-44/A-47: a non-six roll with no legal move passes the dice")
    void noLegalMoveOnNonSixPassesDice() {
        assertEquals(
                List.of("Red player has no legal move with a roll of 4 and passes the dice."),
                MessageTemplates.noLegalMove(new NoLegalMove(Colour.RED, 4)));
    }

    @Test
    @DisplayName("thirdSixIgnored: A-44 wording")
    void thirdSixIgnored() {
        assertEquals(
                List.of("Red player rolled a third consecutive six; the roll is ignored and the dice passes "
                        + "to the next player."),
                MessageTemplates.thirdSixIgnored(new ThirdSixIgnored(Colour.RED)));
    }

    @Test
    @DisplayName("blockMoved: A-44 wording names every member by full piece name")
    void blockMoved() {
        List<PieceId> members = List.of(R1, new PieceId(Colour.RED, 2));
        assertEquals(
                List.of("Red moves its block of 2 pieces (R1 and R2) from 5 to 8 by 3 units each in "
                        + "clockwise direction (rolled 6)."),
                MessageTemplates.blockMoved(
                        new BlockMoved(members, new OnTrack(5), new OnTrack(8), 6, 3, Direction.CLOCKWISE)));
    }

    @Test
    @DisplayName("blockadeBroken: A-44 wording, T-6/A-22")
    void blockadeBroken() {
        assertEquals(
                List.of("Red blockade at square 5 breaks: R1 stay, R2 and R3 leave, each moving 3 units in "
                        + "its own original direction."),
                MessageTemplates.blockadeBroken(new BlockadeBroken(
                        Colour.RED, 5, List.of(R1),
                        List.of(new PieceId(Colour.RED, 2), new PieceId(Colour.RED, 3)), 3)));
    }

    @Test
    @DisplayName("A-43: roundStatusReported: full multi-line block, header lowercase mid-sentence, §3")
    void roundStatusReported() {
        List<PieceLocation> locations = List.of(
                new PieceLocation(new PieceId(Colour.RED, 1), new OnTrack(5)),
                new PieceLocation(new PieceId(Colour.RED, 2), new InBase()),
                new PieceLocation(new PieceId(Colour.RED, 3), new AtHome()));

        assertEquals(
                List.of(
                        "Red player now has 1/4 on pieces on the board and 2/4 pieces on the base.",
                        "============================",
                        "Location of pieces red",
                        "============================",
                        "Piece 1 -> 5.",
                        "Piece 2 -> Base.",
                        "Piece 3 -> Home."),
                MessageTemplates.roundStatusReported(new RoundStatusReported(Colour.RED, 1, 2, locations)));
    }

    @Test
    @DisplayName("A-02: home-straight cells are named <colour>homepath<n>")
    void a02_homeStraightSquareNaming() {
        List<PieceLocation> locations = List.of(new PieceLocation(R1, new InHomeStraight(2)));

        assertEquals(
                List.of(
                        "Red player now has 1/4 on pieces on the board and 3/4 pieces on the base.",
                        "============================",
                        "Location of pieces red",
                        "============================",
                        "Piece 1 -> redhomepath2."),
                MessageTemplates.roundStatusReported(new RoundStatusReported(Colour.RED, 1, 3, locations)));
    }

    @Test
    @DisplayName("A-43: mysterySpawned: verbatim, §3/A-28")
    void mysterySpawned() {
        assertEquals(
                List.of("A mystery cell has spawned in location 17 and will be at this location for the next "
                        + "four rounds."),
                MessageTemplates.mysterySpawned(new MysterySpawned(17)));
    }

    @Test
    @DisplayName("A-43: mysteryCellStatusReported: verbatim, §3/A-28")
    void mysteryCellStatusReported() {
        assertEquals(
                List.of("The mystery cell is at 17 and will be at that location for the next 3 values."),
                MessageTemplates.mysteryCellStatusReported(new MysteryCellStatusReported(17, 3)));
    }

    @Test
    @DisplayName("A-43: mysteryCellTriggered: verbatim, §3/T-11, names the drawn destination")
    void mysteryCellTriggered() {
        assertEquals(
                List.of("Red player lands on a mystery cell and is teleported to Alpha."),
                MessageTemplates.mysteryCellTriggered(new MysteryCellTriggered(R1, MysteryOutcomeKind.ALPHA)));
    }

    @Test
    @DisplayName("A-43: pieceTeleported: not redirected, reuses the kind-specific §3 template")
    void pieceTeleportedNotRedirected() {
        assertEquals(
                List.of("Red piece 1 teleported to Approach."),
                MessageTemplates.pieceTeleported(new PieceTeleported(
                        R1, MysteryOutcomeKind.APPROACH, new OnTrack(24), false, Optional.empty())));
    }

    @Test
    @DisplayName("A-60: a redirected teleport explains the redirect, then reuses the Base template")
    void a60_pieceTeleportedRedirectedToBase() {
        assertEquals(
                List.of(
                        "Alpha is occupied by a green blockade, so red piece 1 is sent to base instead.",
                        "Red piece 1 teleported to Base."),
                MessageTemplates.pieceTeleported(new PieceTeleported(
                        R1, MysteryOutcomeKind.ALPHA, new InBase(), true, Optional.of(Colour.GREEN))));
    }

    @Test
    @DisplayName("A-43: alphaEffectAssigned: energised, verbatim, §3/T-12")
    void alphaEffectAssignedEnergised() {
        assertEquals(
                List.of("Red piece 1 feels energized, and movement speed doubles."),
                MessageTemplates.alphaEffectAssigned(new AlphaEffectAssigned(R1, AlphaEffectKind.ENERGISED)));
    }

    @Test
    @DisplayName("A-43: alphaEffectAssigned: sick, verbatim, §3/T-12")
    void alphaEffectAssignedSick() {
        assertEquals(
                List.of("Red piece 1 feels sick, and movement speed halves."),
                MessageTemplates.alphaEffectAssigned(new AlphaEffectAssigned(R1, AlphaEffectKind.SICK)));
    }

    @Test
    @DisplayName("A-43: briefingAssigned: verbatim, §3/T-13")
    void briefingAssigned() {
        assertEquals(
                List.of("Red piece 1 attends briefing and cannot move for four rounds."),
                MessageTemplates.briefingAssigned(new BriefingAssigned(R1)));
    }

    @Test
    @DisplayName("briefingStreakTriggered: verbatim, §3/A-33/A-56")
    void briefingStreakTriggered() {
        assertEquals(
                List.of("Red piece 1 is movement-restricted and has rolled three consecutively. "
                        + "Teleporting piece 1 to base."),
                MessageTemplates.briefingStreakTriggered(new BriefingStreakTriggered(R1)));
    }

    @Test
    @DisplayName("gammaDirectionReversed: colour lowercase, 'The' opens the sentence, §3/T-14")
    void gammaDirectionReversed() {
        assertEquals(
                List.of("The red piece 1, which was moving clockwise, has changed to moving counterclockwise."),
                MessageTemplates.gammaDirectionReversed(new GammaDirectionReversed(R1)));
    }

    @Test
    @DisplayName("gammaRerouteTriggered: colour lowercase, 'The' opens the sentence, §3/A-58")
    void gammaRerouteTriggered() {
        assertEquals(
                List.of("The red piece 1 is moving in a counterclockwise direction. Teleporting to Beta from Gamma."),
                MessageTemplates.gammaRerouteTriggered(new GammaRerouteTriggered(R1)));
    }

    @Test
    @DisplayName("A-43: playerFinished: place 1 is the verbatim win line, §3")
    void playerFinishedWinner() {
        assertEquals(
                List.of("Red player wins!!!"),
                MessageTemplates.playerFinished(new PlayerFinished(Colour.RED, 1)));
    }

    @Test
    @DisplayName("playerFinished: non-winning places produce no line (reported once, by GameEnded)")
    void playerFinishedNonWinnerIsSilent() {
        assertEquals(List.of(), MessageTemplates.playerFinished(new PlayerFinished(Colour.GREEN, 2)));
        assertEquals(List.of(), MessageTemplates.playerFinished(new PlayerFinished(Colour.GREEN, 3)));
        assertEquals(List.of(), MessageTemplates.playerFinished(new PlayerFinished(Colour.GREEN, 4)));
    }

    @Test
    @DisplayName("A-43: gameEnded normal completion reports 2nd-4th, since 1st already won live")
    void gameEndedNormalCompletionReportsRemainingPlacings() {
        assertEquals(
                List.of(
                        "Green player finishes in 2nd place.",
                        "Yellow player finishes in 3rd place.",
                        "Blue player finishes in 4th place."),
                MessageTemplates.gameEnded(new GameEnded(
                        List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE), false, List.of())));
    }

    @Test
    @DisplayName("A-61: round guard with zero finishers reports 'Finished: none' and everyone not finished")
    void a61_gameEndedRoundGuardWithZeroFinishers() {
        assertEquals(
                List.of("The game reached the 1000-round limit. Finished: none. Did not finish: "
                        + "red, green, yellow, blue."),
                MessageTemplates.gameEnded(new GameEnded(
                        List.of(), true, List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE))));
    }

    @Test
    @DisplayName("A-61: round guard with one live finisher does not repeat its 'wins!!!' placing")
    void a61_gameEndedRoundGuardWithOneFinisher() {
        assertEquals(
                List.of("The game reached the 1000-round limit. Finished: 1st red. Did not finish: "
                        + "green, yellow, blue."),
                MessageTemplates.gameEnded(new GameEnded(
                        List.of(Colour.RED), true, List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE))));
    }

    @Test
    @DisplayName("A-61: round guard with two live finishers lists both in finishing order, not re-announcing 1st")
    void a61_gameEndedRoundGuardWithTwoFinishers() {
        assertEquals(
                List.of("The game reached the 1000-round limit. Finished: 1st red, 2nd green. "
                        + "Did not finish: yellow, blue."),
                MessageTemplates.gameEnded(new GameEnded(
                        List.of(Colour.RED, Colour.GREEN), true, List.of(Colour.YELLOW, Colour.BLUE))));
    }
}
