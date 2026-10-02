package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.PieceMoved;

import java.util.List;

/**
 * Rule 1: a piece already on the standard path (or home straight) moves by {@code units} cells
 * — the roll, or A-32's effect-adjusted value when the piece is energised or sick.
 */
public record StepMove(
        PieceId pieceId,
        Position origin,
        Position destination,
        int units,
        Direction direction,
        boolean capturesSomething,
        boolean formsBlock,
        boolean breaksBlock,
        boolean crossesApproachWithoutEntering,
        boolean landsOnMystery) implements Move {

    @Override
    public List<PieceId> pieceIds() {
        return List.of(pieceId);
    }

    @Override
    public MoveResult execute(MoveContext context) {
        BoardState board = context.board();
        board.moveTo(pieceId, destination);
        if (crossesApproachWithoutEntering) {
            board.recordApproachCrossing(pieceId); // A-08
        }
        context.events().publish(new PieceMoved(pieceId, origin, destination, units, direction));

        boolean captured = false;
        if (capturesSomething) {
            // A-46: the move message is published before the capture message.
            LandingResult landing =
                    context.landingHandler().resolveLanding(List.of(pieceId), destination, board, context.events());
            captured = landing.captured();
        }
        if (landsOnMystery) {
            // A-29: only an individual piece ending its move on the mystery cell triggers T-11.
            boolean teleportCaptured = context.mysteryHandler().trigger(pieceId, board, context.events());
            captured = captured || teleportCaptured;
        }
        return new MoveResult(captured);
    }
}
