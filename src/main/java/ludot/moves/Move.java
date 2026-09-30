package ludot.moves;

import ludot.domain.PieceId;
import ludot.domain.Position;

/**
 * Command object describing one legal move: generated, inspected by a
 * strategy, then executed (Command, DESIGN.md 4.2). {@code permits} grows as
 * later phases add block/partial/mystery variants (T-3, T-4, T-11).
 */
public sealed interface Move permits EnterFromBase, StepMove {

    PieceId pieceId();

    Position origin();

    Position destination();

    boolean capturesSomething();

    boolean formsBlock();

    boolean landsOnMystery();

    MoveResult execute(MoveContext context);
}
