package ludot.rules;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;
import ludot.events.PieceCaptured;
import ludot.events.PieceCountStatus;
import ludot.moves.LandingHandler;
import ludot.moves.LandingResult;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * What happens when a piece lands on a cell: Rule 6 single-piece captures,
 * A-19 block-vs-single captures, and A-20/T-8 block-vs-block captures (T-11
 * mystery-cell landings extend this later). Only ever called when
 * {@code destination} is a standard-track cell holding at least one opponent
 * piece — own-colour and (different-size-block) landings are excluded before
 * a {@code Move} is generated (Rule 7, A-16, A-50). By the time this runs,
 * every mover has already joined {@code destination} (see each {@code Move}'s
 * {@code execute}), so the opponent occupant(s) being captured are identified
 * by colour, not by list position.
 */
public final class LandingResolver implements LandingHandler {

    @Override
    public LandingResult resolveLanding(
            List<PieceId> moverIds, Position destination, BoardState board, EventBus events) {
        if (!(destination instanceof OnTrack trackCell)) {
            throw new IllegalStateException(
                    "LandingResolver only resolves standard-track landings, got " + destination);
        }
        Colour moverColour = moverIds.get(0).colour();
        // A-53: captured in ascending piece-number order, for a deterministic message sequence.
        List<PieceId> captured = board.piecesAt(trackCell.index()).stream()
                .filter(id -> id.colour() != moverColour)
                .sorted(Comparator.comparingInt(PieceId::number))
                .toList();
        // A-51: the capture fact names the lowest-numbered mover (moverIds' first entry) as capturer.
        PieceId capturer = moverIds.get(0);

        // Rule 6/A-19/A-20: every mover is credited with the capture exactly once, regardless of
        // how many opponent pieces are captured in this single landing. A-07/T-7 enforcement (the
        // home-straight gate) is phase 4f.
        for (PieceId moverId : moverIds) {
            board.recordCapture(moverId);
        }

        Colour capturedColour = captured.get(0).colour();
        for (PieceId capturedId : captured) {
            board.resetToBase(capturedId);
            // A-46/A-53: the movement fact was already published before this call; one capture
            // fact plus one count-status line per captured piece follows here.
            events.publish(new PieceCaptured(capturer, destination, capturedId));
            events.publish(new PieceCountStatus(
                    capturedColour, board.countOnBoard(capturedColour), board.countInBase(capturedColour)));
        }

        return new LandingResult(true, Optional.of(captured.get(0)));
    }
}
