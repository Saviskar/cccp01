package ludot.rules;

import ludot.events.PieceBlocked;
import ludot.moves.Move;

import java.util.List;

/**
 * Everything {@link MoveGenerator} learned for one colour and roll:
 * the moves a {@code PlayerStrategy} may choose from, and any piece that was
 * obstructed with no possible partial move (A-16). {@code deadEndObstructions}
 * is only ever non-empty when {@code legalMoves} holds no full move (T-3);
 * whether it should actually be reported (A-48) depends on whether
 * {@code legalMoves} ends up empty too — that decision belongs to
 * {@code TurnController}, since {@link MoveGenerator} never publishes events.
 */
public record MoveGenerationResult(List<Move> legalMoves, List<PieceBlocked> deadEndObstructions) {

    public MoveGenerationResult {
        legalMoves = List.copyOf(legalMoves);
        deadEndObstructions = List.copyOf(deadEndObstructions);
    }
}
