package ludot.moves;

import ludot.domain.PieceId;
import ludot.domain.Position;

import java.util.List;

/**
 * Command object describing one legal move: generated, inspected by a
 * strategy, then executed (Command, DESIGN.md 4.2). T-11's teleport happens
 * inside a triggering {@link StepMove}/{@link PartialMove}'s own
 * {@code execute()}, via {@link MysteryHandler} — no separate {@code Move}
 * implementation is needed for it.
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
