package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.BlockMoved;

import java.util.List;

/**
 * T-4: a block of 2+ same-colour pieces moves together by
 * {@code floor(rollValue / pieceIds.size())} cells each (A-17), staying on
 * the standard track (A-18) and never obstructed partway (A-50 — an
 * obstructed block move is never generated at all).
 */
public record BlockMove(
        List<PieceId> pieceIds,
        Position origin,
        Position destination,
        int rollValue,
        int cellsPerPiece,
        Direction direction,
        boolean capturesSomething,
        boolean crossesApproachWithoutEntering) implements Move {

    public BlockMove {
        pieceIds = List.copyOf(pieceIds);
    }

    @Override
    public boolean formsBlock() {
        return true; // still a block after moving together
    }

    @Override
    public boolean breaksBlock() {
        return false;
    }

    @Override
    public boolean landsOnMystery() {
        return false; // A-29: block moves never trigger the mystery cell
    }

    @Override
    public MoveResult execute(MoveContext context) {
        BoardState board = context.board();
        for (PieceId pieceId : pieceIds) {
            board.moveTo(pieceId, destination);
        }
        if (crossesApproachWithoutEntering) {
            // A-08: every member is credited with the crossing (A-18: a block move never
            // enters the home straight, so this is always a non-entering crossing).
            for (PieceId pieceId : pieceIds) {
                board.recordApproachCrossing(pieceId);
            }
        }
        context.events().publish(new BlockMoved(pieceIds, origin, destination, rollValue, cellsPerPiece, direction));

        if (!capturesSomething) {
            return new MoveResult(false);
        }
        // A-46/A-51: the movement fact is published before the capture fact.
        LandingResult landing = context.landingHandler().resolveLanding(pieceIds, destination, board, context.events());
        return new MoveResult(landing.captured());
    }
}
