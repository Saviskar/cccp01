package ludot.engine;

import ludot.board.BoardState;
import ludot.board.GameView;
import ludot.board.MysteryCellTick;
import ludot.board.Piece;
import ludot.domain.Colour;
import ludot.events.EventBus;
import ludot.events.MysteryCellStatusReported;
import ludot.events.MysterySpawned;
import ludot.events.PieceLocation;
import ludot.events.RoundStatusReported;
import ludot.random.RandomPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Plays one round: every non-finished player's turn, then a status report for every colour. */
public final class RoundManager {

    private final TurnController turnController;
    private final EventBus events;
    private final RandomPicker randomPicker;

    public RoundManager(TurnController turnController, EventBus events, RandomPicker randomPicker) {
        this.turnController = turnController;
        this.events = events;
        this.randomPicker = randomPicker;
    }

    public void playRound(List<Player> playersInOrder, BoardState board, Standings standings, GameView view) {
        for (Player player : playersInOrder) {
            if (!standings.hasFinished(player.colour())) {
                turnController.playTurn(player, board, standings, view);
            }
        }
        // Section 3 doesn't exempt finished colours from the round-end status.
        for (Player player : playersInOrder) {
            reportStatus(player.colour(), board);
        }
        reportMysteryCellStatus(board);
    }

    // A-28: advances the mystery cell's timer once per round, after the per-colour status.
    private void reportMysteryCellStatus(BoardState board) {
        Optional<MysteryCellTick> tick = board.tickMysteryCell(randomPicker);
        if (tick.isEmpty()) {
            return;
        }
        MysteryCellTick value = tick.get();
        if (value.justSpawned()) {
            events.publish(new MysterySpawned(value.location()));
        }
        events.publish(new MysteryCellStatusReported(value.location(), value.roundsRemaining()));
    }

    private void reportStatus(Colour colour, BoardState board) {
        List<PieceLocation> locations = new ArrayList<>();
        for (Piece piece : board.piecesOfColour(colour)) {
            locations.add(new PieceLocation(piece.id(), piece.position()));
        }
        events.publish(new RoundStatusReported(
                colour, board.countOnBoard(colour), board.countInBase(colour), locations));
    }
}
