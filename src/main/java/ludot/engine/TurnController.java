package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.board.GameView;
import ludot.domain.Colour;
import ludot.events.DiceRolled;
import ludot.events.EventBus;
import ludot.events.NoLegalMove;
import ludot.events.PlayerFinished;
import ludot.events.ThirdSixIgnored;
import ludot.moves.LandingHandler;
import ludot.moves.Move;
import ludot.moves.MoveContext;
import ludot.random.Dice;
import ludot.rules.MoveGenerator;

import java.util.List;

/**
 * Plays one player's turn: the roll loop, the six-streak (Rule 4), and
 * detecting when the colour has just finished (Rule 11/A-41). T-2's capture
 * bonus (phase 4b) and T-6's with-a-block variant of Rule 4 (phase 4e, since
 * blocks don't exist yet) are not wired in.
 */
public final class TurnController {

    private static final int SIX = 6;
    private static final int THIRD_CONSECUTIVE_SIX = 3;
    private static final int PIECES_PER_COLOUR = 4;

    private final Dice dice;
    private final MoveGenerator moveGenerator;
    private final BoardTopology topology;
    private final EventBus events;
    private final LandingHandler landingHandler;

    public TurnController(
            Dice dice, MoveGenerator moveGenerator, BoardTopology topology, EventBus events, LandingHandler landingHandler) {
        this.dice = dice;
        this.moveGenerator = moveGenerator;
        this.topology = topology;
        this.events = events;
        this.landingHandler = landingHandler;
    }

    public void playTurn(Player player, BoardState board, Standings standings, GameView view) {
        Colour colour = player.colour();
        int sixStreak = 0;
        while (true) {
            int roll = dice.roll();
            events.publish(new DiceRolled(colour, roll));
            sixStreak = roll == SIX ? sixStreak + 1 : 0;
            if (sixStreak == THIRD_CONSECUTIVE_SIX) {
                events.publish(new ThirdSixIgnored(colour));
                return;
            }

            if (playRoll(player, roll, board, standings, view)) {
                return; // the colour just finished; the turn ends immediately
            }

            if (roll != SIX) {
                return;
            }
            // A-47: a six always grants a bonus roll, even if this roll produced no move.
        }
    }

    /** Returns true if this roll's move brought the colour's last piece Home. */
    private boolean playRoll(Player player, int roll, BoardState board, Standings standings, GameView view) {
        Colour colour = player.colour();
        List<Move> moves = moveGenerator.legalMoves(colour, roll, board, topology);
        if (moves.isEmpty()) {
            events.publish(new NoLegalMove(colour, roll));
            return false;
        }

        Move chosen = player.strategy().choose(moves, view);
        if (!moves.contains(chosen)) {
            throw new IllegalStateException("Strategy chose a move outside the legal list: " + chosen);
        }
        chosen.execute(new MoveContext(board, events, landingHandler));

        if (board.countAtHome(colour) != PIECES_PER_COLOUR) {
            return false;
        }
        standings.recordFinish(colour);
        events.publish(new PlayerFinished(colour, standings.finishOrder().size()));
        return true;
    }
}
