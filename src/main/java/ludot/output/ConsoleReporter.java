package ludot.output;

import ludot.events.AlphaEffectAssigned;
import ludot.events.BlockMoved;
import ludot.events.BlockadeBroken;
import ludot.events.BriefingAssigned;
import ludot.events.BriefingStreakTriggered;
import ludot.events.DiceRolled;
import ludot.events.GameEnded;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
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
import ludot.events.PieceMoved;
import ludot.events.PiecePartiallyMoved;
import ludot.events.PieceTeleported;
import ludot.events.PiecesIntroduced;
import ludot.events.PlayerFinished;
import ludot.events.RoundOrderAnnounced;
import ludot.events.RoundStatusReported;
import ludot.events.ThirdSixIgnored;
import ludot.events.ThrowIgnoredAfterBlock;

import java.io.PrintStream;
import java.util.List;

/**
 * Observer (DESIGN.md 4.3): the only class that prints. Formats each published
 * {@link GameEvent} via {@link MessageTemplates} and writes one line at a time.
 * The {@link PrintStream} is constructor-injected, the same way {@code Dice}/
 * {@code Coin}/{@code RandomPicker} are (DESIGN.md 4.6), so tests can capture
 * output without redirecting the real {@code System.out}.
 */
public final class ConsoleReporter implements GameEventListener {

    private final PrintStream out;

    public ConsoleReporter(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onEvent(GameEvent event) {
        List<String> lines = switch (event) {
            case PiecesIntroduced e -> MessageTemplates.piecesIntroduced(e);
            case OpeningRollRolled e -> MessageTemplates.openingRollRolled(e);
            case OpeningRollWinnerDetermined e -> MessageTemplates.openingRollWinnerDetermined(e);
            case RoundOrderAnnounced e -> MessageTemplates.roundOrderAnnounced(e);
            case DiceRolled e -> MessageTemplates.diceRolled(e);
            case PieceEnteredX e -> MessageTemplates.pieceEnteredX(e);
            case PieceDirectionAssigned e -> MessageTemplates.pieceDirectionAssigned(e);
            case PieceMoved e -> MessageTemplates.pieceMoved(e);
            case BlockMoved e -> MessageTemplates.blockMoved(e);
            case PieceCaptured e -> MessageTemplates.pieceCaptured(e);
            case PieceCountStatus e -> MessageTemplates.pieceCountStatus(e);
            case PieceBlocked e -> MessageTemplates.pieceBlocked(e);
            case PiecePartiallyMoved e -> MessageTemplates.piecePartiallyMoved(e);
            case ThrowIgnoredAfterBlock e -> MessageTemplates.throwIgnoredAfterBlock(e);
            case NoLegalMove e -> MessageTemplates.noLegalMove(e);
            case ThirdSixIgnored e -> MessageTemplates.thirdSixIgnored(e);
            case BlockadeBroken e -> MessageTemplates.blockadeBroken(e);
            case RoundStatusReported e -> MessageTemplates.roundStatusReported(e);
            case PlayerFinished e -> MessageTemplates.playerFinished(e);
            case GameEnded e -> MessageTemplates.gameEnded(e);
            case MysterySpawned e -> MessageTemplates.mysterySpawned(e);
            case MysteryCellStatusReported e -> MessageTemplates.mysteryCellStatusReported(e);
            case MysteryCellTriggered e -> MessageTemplates.mysteryCellTriggered(e);
            case PieceTeleported e -> MessageTemplates.pieceTeleported(e);
            case AlphaEffectAssigned e -> MessageTemplates.alphaEffectAssigned(e);
            case BriefingAssigned e -> MessageTemplates.briefingAssigned(e);
            case BriefingStreakTriggered e -> MessageTemplates.briefingStreakTriggered(e);
            case GammaDirectionReversed e -> MessageTemplates.gammaDirectionReversed(e);
            case GammaRerouteTriggered e -> MessageTemplates.gammaRerouteTriggered(e);
        };
        lines.forEach(out::println);
    }
}
