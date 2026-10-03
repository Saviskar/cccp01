package ludot.app;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.PieceId;
import ludot.events.GameEnded;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
import ludot.events.RoundStatusReported;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLAUDE.md phase 7 / DESIGN.md §10: 1,000 seeded games with the real strategies, asserting the
 * board-conservation and no-shared-track-cell invariants hold at the end of every round (not
 * just at game end), that every game terminates, and that nothing throws.
 */
class GameFactoryStressTest {

    private static final int GAME_COUNT = 1000;
    private static final int PIECES_PER_COLOUR = 4;
    private static final int TRACK_SIZE = 52;
    private static final int COLOURS_PER_ROUND = Colour.values().length;

    @Test
    @DisplayName("1,000 seeded games: board invariants hold every round, every game terminates")
    void a41A42_thousandSeededGamesHoldInvariantsAndTerminate() {
        for (long seed = 0; seed < GAME_COUNT; seed++) {
            GameSession session = GameFactory.newGame(seed);
            AtomicInteger gameEndedCount = new AtomicInteger();
            session.events().subscribe(new EndOfRoundInvariantListener(session.board(), seed, gameEndedCount));

            session.run();

            assertEquals(1, gameEndedCount.get(), "seed " + seed + ": expected exactly one GameEnded");
        }
    }

    /**
     * Holds a direct reference to the live {@link BoardState}, so it can check real occupancy
     * rather than reconstructing it from events. End of round is detected by counting
     * {@link RoundStatusReported} events: exactly one is published per colour per round, in a
     * fixed order, so every 4th one marks a round boundary.
     */
    private static final class EndOfRoundInvariantListener implements GameEventListener {

        private final BoardState board;
        private final long seed;
        private final AtomicInteger gameEndedCount;
        private int statusCount;

        EndOfRoundInvariantListener(BoardState board, long seed, AtomicInteger gameEndedCount) {
            this.board = board;
            this.seed = seed;
            this.gameEndedCount = gameEndedCount;
        }

        @Override
        public void onEvent(GameEvent event) {
            if (event instanceof RoundStatusReported) {
                statusCount++;
                if (statusCount % COLOURS_PER_ROUND == 0) {
                    checkInvariants(statusCount / COLOURS_PER_ROUND);
                }
            } else if (event instanceof GameEnded) {
                gameEndedCount.incrementAndGet();
            }
        }

        private void checkInvariants(int round) {
            for (Colour colour : Colour.values()) {
                int total = board.countInBase(colour) + board.countOnBoard(colour) + board.countAtHome(colour);
                assertEquals(PIECES_PER_COLOUR, total,
                        () -> "seed " + seed + " round " + round + ": " + colour + " has " + total
                                + " pieces, expected " + PIECES_PER_COLOUR);
            }
            for (int cell = 0; cell < TRACK_SIZE; cell++) {
                List<PieceId> occupants = board.piecesAt(cell);
                if (occupants.isEmpty()) {
                    continue;
                }
                Colour first = occupants.get(0).colour();
                boolean singleColour = occupants.stream().allMatch(id -> id.colour() == first);
                int finalCell = cell;
                assertTrue(singleColour,
                        () -> "seed " + seed + " round " + round + ": track cell " + finalCell
                                + " holds pieces of more than one colour: " + occupants);
            }
        }
    }
}
