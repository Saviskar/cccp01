package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.PieceEnteredX;

/** Rule 2: a piece moves from base to its colour's starting square X. */
public record EnterFromBase(PieceId pieceId, Position destination, boolean capturesSomething) implements Move {

    @Override
    public Position origin() {
        return new InBase();
    }

    @Override
    public boolean formsBlock() {
        return false;
    }

    @Override
    public boolean landsOnMystery() {
        return false;
    }

    @Override
    public MoveResult execute(MoveContext context) {
        BoardState board = context.board();
        board.moveTo(pieceId, destination);
        // A-12: the coin toss that replaces this fixed direction arrives with T-1 in phase 4a.
        board.assignDirection(pieceId, Direction.CLOCKWISE);
        context.events().publish(new PieceEnteredX(
                pieceId, board.countOnBoard(pieceId.colour()), board.countInBase(pieceId.colour())));

        if (!capturesSomething) {
            return new MoveResult(false);
        }
        // A-46: the "moved to the starting point" message is published before the capture message.
        LandingResult landing = context.landingHandler().resolveLanding(pieceId, destination, board, context.events());
        return new MoveResult(landing.captured());
    }
}
