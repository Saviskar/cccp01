package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.PieceDirectionAssigned;
import ludot.events.PieceEnteredX;

/** Rule 2: a piece moves from base to its colour's starting square X. */
public record EnterFromBase(PieceId pieceId, Position destination, boolean capturesSomething, boolean formsBlock)
        implements Move {

    @Override
    public Position origin() {
        return new InBase();
    }

    @Override
    public boolean landsOnMystery() {
        return false;
    }

    @Override
    public MoveResult execute(MoveContext context) {
        BoardState board = context.board();
        board.moveTo(pieceId, destination);
        context.events().publish(new PieceEnteredX(
                pieceId, board.countOnBoard(pieceId.colour()), board.countInBase(pieceId.colour())));

        // A-12: the coin toss happens only now, once the piece has reached X.
        Direction direction = context.coin().toss();
        board.assignDirection(pieceId, direction);
        context.events().publish(new PieceDirectionAssigned(pieceId, direction));

        if (!capturesSomething) {
            return new MoveResult(false);
        }
        // A-46: the "moved to the starting point" message is published before the capture message.
        LandingResult landing = context.landingHandler().resolveLanding(pieceId, destination, board, context.events());
        return new MoveResult(landing.captured());
    }
}
