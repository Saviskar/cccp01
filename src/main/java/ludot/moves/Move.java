package ludot.moves;

import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.List;

/**
 * Command object describing one legal move: generated, inspected by a
 * strategy, then executed (Command, DESIGN.md 4.2). {@code permits} grows as
 * later phases add mystery variants (T-11).
 */
public sealed interface Move permits EnterFromBase, StepMove, PartialMove, BlockMove {

    List<PieceId> pieceIds();

    Position origin();

    Position destination();

    boolean capturesSomething();

    boolean formsBlock();

    /** T-5/A-21: true when this move's origin cell currently holds a block of the mover's own colour. */
    boolean breaksBlock();

    boolean landsOnMystery();

    MoveResult execute(MoveContext context);
}
