package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.PieceMoved;

import java.util.List;

/** Rule 1: a piece already on the standard path (or home straight) moves by the roll value. */
public record StepMove(
        PieceId pieceId,
        Position origin,
        Position destination,
        int rollValue,
        Direction direction,
        boolean capturesSomething,
        boolean formsBlock,
        boolean breaksBlock,
        boolean crossesApproachWithoutEntering) implements Move {

    @Override
    public List<PieceId> pieceIds() {
        return List.of(pieceId);
    }

    @Override
    public boolean landsOnMystery() {
        return false;
    }

    @Override
    public MoveResult execute(MoveContext context) {
        BoardState board = context.board();
        board.moveTo(pieceId, destination);
        if (crossesApproachWithoutEntering) {
            board.recordApproachCrossing(pieceId); // A-08
        }
        context.events().publish(new PieceMoved(pieceId, origin, destination, rollValue, direction));

        if (!capturesSomething) {
            return new MoveResult(false);
        }
        // A-46: the move message is published before the capture message.
        LandingResult landing =
                context.landingHandler().resolveLanding(List.of(pieceId), destination, board, context.events());
        return new MoveResult(landing.captured());
    }
}
