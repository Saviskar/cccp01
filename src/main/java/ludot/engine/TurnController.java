package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.board.GameView;
import ludot.board.Piece;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.PieceId;
import ludot.events.BlockadeBroken;
import ludot.events.DiceRolled;
import ludot.events.EventBus;
import ludot.events.NoLegalMove;
import ludot.events.PieceBlocked;
import ludot.events.PlayerFinished;
import ludot.events.ThirdSixIgnored;
import ludot.events.ThrowIgnoredAfterBlock;
import ludot.moves.LandingHandler;
import ludot.moves.Move;
import ludot.moves.MoveContext;
import ludot.moves.MoveResult;
import ludot.random.Coin;
import ludot.random.Dice;
import ludot.rules.BlockBreak;
import ludot.rules.BlockBreakPlanner;
import ludot.rules.ForcedMoveOutcome;
import ludot.rules.MoveGenerationResult;
import ludot.rules.MoveGenerator;

import java.util.List;

/**
 * Plays one player's turn: the roll loop, the six-streak (Rule 4), T-2's
 * capture bonus roll (A-23), T-6's with-a-block variant of Rule 4 (A-22), and
 * detecting when the colour has just finished (Rule 11/A-41).
 */
public final class TurnController {

    private static final int SIX = 6;
    private static final int THIRD_CONSECUTIVE_SIX = 3;
    private static final int PIECES_PER_COLOUR = 4;

    private final Dice dice;
    private final Coin coin;
    private final MoveGenerator moveGenerator;
    private final BoardTopology topology;
    private final EventBus events;
    private final LandingHandler landingHandler;
    private final BlockBreakPlanner blockBreakPlanner;

    public TurnController(
            Dice dice, Coin coin, MoveGenerator moveGenerator, BoardTopology topology, EventBus events,
            LandingHandler landingHandler, BlockBreakPlanner blockBreakPlanner) {
        this.dice = dice;
        this.coin = coin;
        this.moveGenerator = moveGenerator;
        this.topology = topology;
        this.events = events;
        this.landingHandler = landingHandler;
        this.blockBreakPlanner = blockBreakPlanner;
    }

    public void playTurn(Player player, BoardState board, Standings standings, GameView view) {
        Colour colour = player.colour();
        int sixStreak = 0;
        while (true) {
            int roll = dice.roll();
            events.publish(new DiceRolled(colour, roll));
            // A-24: resets on any non-six roll; a bonus roll earned via capture is a normal roll here too.
            sixStreak = roll == SIX ? sixStreak + 1 : 0;
            if (sixStreak == THIRD_CONSECUTIVE_SIX) {
                events.publish(new ThirdSixIgnored(colour));
                breakBlocksIfAny(colour, board, topology);
                return; // A-22: the turn has ended -- no bonus roll even if a break captures something
            }

            RollOutcome outcome = playRoll(player, roll, board, standings, view);
            if (outcome.finished() || outcome.forceEndTurn()) {
                return; // the colour just finished, or A-47's exception forces the turn to end
            }

            if (roll != SIX && !outcome.captured()) {
                return;
            }
            // A-23: a six or a capture grants exactly one bonus roll; this is a single OR,
            // so a roll that is both a six and a capture still grants only one bonus roll.
        }
    }

    private RollOutcome playRoll(Player player, int roll, BoardState board, Standings standings, GameView view) {
        Colour colour = player.colour();
        MoveGenerationResult generation = moveGenerator.legalMoves(colour, roll, board, topology);
        List<Move> moves = generation.legalMoves();
        if (moves.isEmpty()) {
            return reportNoMove(colour, roll, generation.deadEndObstructions());
        }

        Move chosen = player.strategy().choose(moves, view);
        if (!moves.contains(chosen)) {
            throw new IllegalStateException("Strategy chose a move outside the legal list: " + chosen);
        }
        MoveResult result = chosen.execute(new MoveContext(board, events, landingHandler, coin));

        if (board.countAtHome(colour) != PIECES_PER_COLOUR) {
            return new RollOutcome(result.captured(), false, false);
        }
        standings.recordFinish(colour);
        events.publish(new PlayerFinished(colour, standings.finishOrder().size()));
        return new RollOutcome(result.captured(), true, false);
    }

    // A-48: each dead-end obstruction publishes its "is blocked" fact, followed by a single
    // per-turn "ignoring the throw" fact — and NoLegalMove is skipped. A-47's exception: this
    // forces the turn to end even if the roll was a six.
    private RollOutcome reportNoMove(Colour colour, int roll, List<PieceBlocked> deadEnds) {
        if (deadEnds.isEmpty()) {
            events.publish(new NoLegalMove(colour, roll));
            return new RollOutcome(false, false, false);
        }
        deadEnds.forEach(events::publish);
        events.publish(new ThrowIgnoredAfterBlock(colour));
        return new RollOutcome(false, false, true);
    }

    /**
     * Whether the executed move captured an opponent piece, whether the colour just finished,
     * and whether the turn must end regardless (A-47's obstruction exception).
     */
    private record RollOutcome(boolean captured, boolean finished, boolean forceEndTurn) {
    }

    // T-6/A-22: breaks every block the colour owns, using a snapshot taken before any member
    // moves (BlockBreakPlanner), so a leaver landing on another of the colour's own blocks
    // mid-sequence can't be mistaken for a newly discovered or resized block. No PlayerFinished
    // check is needed afterwards: each block's staying member never moves, so at least one of the
    // colour's pieces is guaranteed not to reach Home as part of any single break.
    private void breakBlocksIfAny(Colour colour, BoardState board, BoardTopology topology) {
        List<BlockBreak> plan = blockBreakPlanner.plan(colour, board, topology);
        for (BlockBreak blockBreak : plan) {
            events.publish(new BlockadeBroken(
                    colour, blockBreak.cell(), blockBreak.staying(), blockBreak.leaving(), blockBreak.unitsEach()));
            for (PieceId id : blockBreak.leaving()) {
                executeForcedMove(board.piece(id), blockBreak.unitsEach(), board, topology);
            }
        }
    }

    // Generated fresh, right before executing: the walk must see captures and vacated cells left
    // by any earlier leaver in this same sequence, even though the snapshot itself does not change.
    private void executeForcedMove(Piece piece, int units, BoardState board, BoardTopology topology) {
        Direction direction = piece.originalDirection().orElseThrow();
        ForcedMoveOutcome outcome = moveGenerator.forcedMove(piece, units, direction, true, board, topology);
        switch (outcome) {
            case ForcedMoveOutcome.DeadEnd deadEnd -> events.publish(deadEnd.blocked()); // A-52
            case ForcedMoveOutcome.Movable movable ->
                    movable.move().execute(new MoveContext(board, events, landingHandler, coin));
        }
    }
}
