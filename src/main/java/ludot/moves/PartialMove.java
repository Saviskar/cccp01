package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.PieceBlocked;
import ludot.events.PiecePartiallyMoved;

import java.util.List;

/**
 * A-16: a piece obstructed by an opponent block moves only as far as the cell
 * before it, and only when the player has no other legal full move. Per
 * A-48, this is the only move that reports the obstruction: it publishes the
 * "is blocked" fact and the partial-move fact itself, tied to actually being
 * chosen and executed.
 */
public record PartialMove(
        PieceId pieceId,
        Position origin,
        Position destination,
        Position intendedDestination,
        PieceId blockingPieceId,
        int rollValue,
        int cellsMoved,
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
        context.events().publish(new PieceBlocked(pieceId, origin, intendedDestination, blockingPieceId));

        board.moveTo(pieceId, destination);
        if (crossesApproachWithoutEntering) {
            board.recordApproachCrossing(pieceId); // A-08
        }
        context.events().publish(new PiecePartiallyMoved(pieceId, origin, destination, cellsMoved, direction));

        if (!capturesSomething) {
            return new MoveResult(false);
        }
        // A-46: the movement fact is published before the capture fact.
        LandingResult landing =
                context.landingHandler().resolveLanding(List.of(pieceId), destination, board, context.events());
        return new MoveResult(landing.captured());
    }
}
