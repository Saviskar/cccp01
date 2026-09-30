package ludot.rules;

import ludot.board.BoardState;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;
import ludot.events.PieceCaptured;
import ludot.events.PieceCountStatus;
import ludot.moves.LandingHandler;
import ludot.moves.LandingResult;

import java.util.List;
import java.util.Optional;

/**
 * What happens when a piece lands on a cell: currently just Rule 6 captures
 * (T-8 block captures and T-11 mystery-cell landings extend this later).
 * Only ever called when {@code destination} is a standard-track cell holding
 * a single opponent piece — own-colour and block landings are excluded before
 * a {@code Move} is generated (Rule 7, A-16).
 */
public final class LandingResolver implements LandingHandler {

    @Override
    public LandingResult resolveLanding(PieceId moverId, Position destination, BoardState board, EventBus events) {
        if (!(destination instanceof OnTrack trackCell)) {
            throw new IllegalStateException(
                    "LandingResolver only resolves standard-track landings, got " + destination);
        }
        List<PieceId> occupants = board.piecesAt(trackCell.index());
        PieceId captured = occupants.get(0);

        board.resetToBase(captured);
        // Rule 6: record the capture on the capturing piece. A-07 / T-7 enforcement (the
        // home-straight gate) is phase 4f.
        board.recordCapture(moverId);

        events.publish(new PieceCaptured(moverId, destination, captured));
        events.publish(new PieceCountStatus(
                captured.colour(), board.countOnBoard(captured.colour()), board.countInBase(captured.colour())));

        return new LandingResult(true, Optional.of(captured));
    }
}
