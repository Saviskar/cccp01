package ludot.rules;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;
import ludot.events.PieceTeleported;
import ludot.moves.LandingHandler;
import ludot.moves.LandingResult;

import java.util.List;

/**
 * A-31: shared landing logic for every {@link MysteryOutcome} that teleports to
 * a fixed standard-track cell (Alpha, Beta, Gamma, X, Approach). {@link TeleportToBase}
 * has no landing cell, so it does not use this.
 */
final class MysteryLanding {

    private MysteryLanding() {
    }

    static boolean landAt(
            PieceId pieceId, int trackIndex, MysteryOutcomeKind kind, BoardState board, LandingHandler landingHandler,
            EventBus events) {
        Colour moverColour = pieceId.colour();
        if (board.isBlock(trackIndex) && board.colourAt(trackIndex).orElseThrow() != moverColour) {
            // A-31: an opponent block sends the teleported piece to base instead of landing there.
            board.resetToBase(pieceId); // A-26/T-9: a return to base always resets everything
            events.publish(new PieceTeleported(pieceId, kind, new InBase(), true));
            return false;
        }
        boolean captures = board.colourAt(trackIndex).map(colour -> colour != moverColour).orElse(false);
        Position destination = new OnTrack(trackIndex);
        board.moveTo(pieceId, destination);
        events.publish(new PieceTeleported(pieceId, kind, destination, false));
        if (!captures) {
            return false; // empty cell, or an own piece forms a block (A-14) -- neither captures
        }
        LandingResult landing = landingHandler.resolveLanding(List.of(pieceId), destination, board, events);
        return landing.captured();
    }
}
