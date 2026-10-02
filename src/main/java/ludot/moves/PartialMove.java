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
        int units, // A-32: the effective units attempted, before the block cut it short; distinct from cellsMoved below
        int cellsMoved,
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
        context.events().publish(new PieceBlocked(pieceId, origin, intendedDestination, blockingPieceId));

        board.moveTo(pieceId, destination);
        if (crossesApproachWithoutEntering) {
            board.recordApproachCrossing(pieceId); // A-08
        }
        context.events().publish(new PiecePartiallyMoved(pieceId, origin, destination, cellsMoved, direction));

        boolean captured = false;
        if (capturesSomething) {
            // A-46: the movement fact is published before the capture fact.
            LandingResult landing =
                    context.landingHandler().resolveLanding(List.of(pieceId), destination, board, context.events());
            captured = landing.captured();
        }
        if (landsOnMystery) {
            // A-29: only an individual piece ending its move on the mystery cell triggers T-11. This
            // also covers a T-6 forced-break member (A-54), since MoveGenerator.forcedMove reuses the
            // same buildStepMove/buildPartialMove helpers as an ordinary roll.
            boolean teleportCaptured = context.mysteryHandler().trigger(pieceId, board, context.events());
            captured = captured || teleportCaptured;
        }
        return new MoveResult(captured);
    }
}
