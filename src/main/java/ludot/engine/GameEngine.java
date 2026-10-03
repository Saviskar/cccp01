package ludot.engine;

import ludot.board.BoardState;
import ludot.board.GameView;
import ludot.board.Piece;
import ludot.domain.Colour;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.GameEnded;
import ludot.events.OpeningRollRolled;
import ludot.events.OpeningRollWinnerDetermined;
import ludot.events.PiecesIntroduced;
import ludot.events.RoundOrderAnnounced;
import ludot.events.SeedSelected;
import ludot.players.PlayerStrategy;
import ludot.random.Dice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Opening roll, round loop, and the Rule 11/A-41/A-42 end of the game. */
public final class GameEngine {

    // A-42: the game ends after 1000 rounds if it isn't already complete.
    public static final int MAX_ROUNDS = 1000;

    // A-04: fixed round order, kept separate from Colour's enum declaration order (DESIGN.md 3.1).
    private static final List<Colour> TURN_ORDER = List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE);

    private static final int PIECES_PER_COLOUR = 4;

    private final long seed;
    private final Dice dice;
    private final RoundManager roundManager;
    private final EventBus events;
    private final Map<Colour, PlayerStrategy> strategies;

    public GameEngine(
            long seed, Dice dice, RoundManager roundManager, EventBus events, Map<Colour, PlayerStrategy> strategies) {
        this.seed = seed;
        this.dice = dice;
        this.roundManager = roundManager;
        this.events = events;
        this.strategies = new EnumMap<>(strategies);
    }

    public void run(BoardState board, GameView view) {
        // A-72: announces the seed this run used (given or defaulted by Main), so every run is
        // replayable; published here, not by Main, so the engine stays the sole event publisher.
        events.publish(new SeedSelected(seed));
        introducePieces(board);

        Colour winner = openingRoll();
        events.publish(new OpeningRollWinnerDetermined(winner));
        List<Colour> order = rotateStartingAt(TURN_ORDER, winner);
        events.publish(new RoundOrderAnnounced(order));
        List<Player> players = order.stream()
                .map(colour -> new Player(colour, strategies.get(colour)))
                .toList();

        Standings standings = new Standings();
        int round = 0;
        while (!standings.isOver() && round < MAX_ROUNDS) {
            roundManager.playRound(players, board, standings, view);
            round++;
        }

        boolean stoppedByRoundGuard = !standings.isOver();
        List<Colour> notFinished = TURN_ORDER.stream().filter(colour -> !standings.hasFinished(colour)).toList();
        events.publish(new GameEnded(standings.finalPlacings(TURN_ORDER), stoppedByRoundGuard, notFinished));
    }

    private void introducePieces(BoardState board) {
        for (Colour colour : TURN_ORDER) {
            List<PieceId> ids = new ArrayList<>(PIECES_PER_COLOUR);
            for (Piece piece : board.piecesOfColour(colour)) {
                ids.add(piece.id());
            }
            events.publish(new PiecesIntroduced(colour, ids));
        }
    }

    /** A-27: only the tied colours re-roll; every roll, including re-rolls, is published. */
    private Colour openingRoll() {
        List<Colour> candidates = TURN_ORDER;
        while (true) {
            Map<Colour, Integer> rolls = new EnumMap<>(Colour.class);
            for (Colour colour : candidates) {
                int value = dice.roll();
                events.publish(new OpeningRollRolled(colour, value));
                rolls.put(colour, value);
            }
            int max = rolls.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
            List<Colour> tied = candidates.stream().filter(colour -> rolls.get(colour) == max).toList();
            if (tied.size() == 1) {
                return tied.get(0);
            }
            candidates = tied;
        }
    }

    private List<Colour> rotateStartingAt(List<Colour> order, Colour start) {
        int index = order.indexOf(start);
        List<Colour> rotated = new ArrayList<>(order.size());
        for (int i = 0; i < order.size(); i++) {
            rotated.add(order.get((index + i) % order.size()));
        }
        return rotated;
    }
}
