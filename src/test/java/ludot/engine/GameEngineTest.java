package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.events.EventBus;
import ludot.events.GameEnded;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
import ludot.events.OpeningRollRolled;
import ludot.events.OpeningRollWinnerDetermined;
import ludot.events.RoundOrderAnnounced;
import ludot.players.FirstLegalMoveStrategy;
import ludot.players.PlayerStrategy;
import ludot.random.Dice;
import ludot.random.RandomPicker;
import ludot.rules.BlockBreakPlanner;
import ludot.rules.LandingResolver;
import ludot.rules.MoveGenerator;
import ludot.rules.MovementCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameEngineTest {

    @Mock
    private Dice dice;

    @Mock
    private GameEventListener listener;

    @Mock
    private RandomPicker randomPicker;

    private GameEngine engine;
    private BoardState board;

    @BeforeEach
    void setUp() {
        BoardTopology topology = new BoardTopology();
        EventBus events = new EventBus();
        events.subscribe(listener);
        // Plumbing only: no GameEngineTest scenario reaches EnterFromBase (all trailing roll
        // values are non-six), so a lambda avoids an unused Mockito stub tripping strict-stubs.
        // Likewise, no piece ever reaches the standard track, so the mystery-cell timer never
        // starts and neither handler below is ever actually invoked.
        TurnController turnController = new TurnController(
                dice, () -> Direction.CLOCKWISE, new MoveGenerator(new MovementCalculator()), topology, events,
                new LandingResolver(), new BlockBreakPlanner(), (pieceId, b, e) -> false);
        RoundManager roundManager = new RoundManager(turnController, events, randomPicker);
        Map<Colour, PlayerStrategy> strategies = new EnumMap<>(Colour.class);
        for (Colour colour : Colour.values()) {
            strategies.put(colour, new FirstLegalMoveStrategy());
        }
        engine = new GameEngine(dice, roundManager, events, strategies);
        board = new BoardState();
    }

    private <T extends GameEvent> List<T> published(Class<T> type, ArgumentCaptor<GameEvent> captor) {
        return captor.getAllValues().stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Test
    @DisplayName("A-27: only the tied colours re-roll, and every roll (including re-rolls) is published")
    void a27_onlyTiedColoursReRollAndEveryRollIsPublished() {
        // Opening: RED=6, GREEN=6, YELLOW=3, BLUE=2 (RED/GREEN tie); re-roll: RED=5, GREEN=6 (GREEN wins).
        // The trailing 3 keeps every later turn a no-op so the round guard resolves quickly.
        when(dice.roll()).thenReturn(6, 6, 3, 2, 5, 6, 3);

        engine.run(board, board);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());

        List<OpeningRollRolled> rolls = published(OpeningRollRolled.class, captor);
        assertEquals(List.of(
                new OpeningRollRolled(Colour.RED, 6),
                new OpeningRollRolled(Colour.GREEN, 6),
                new OpeningRollRolled(Colour.YELLOW, 3),
                new OpeningRollRolled(Colour.BLUE, 2),
                new OpeningRollRolled(Colour.RED, 5),
                new OpeningRollRolled(Colour.GREEN, 6)), rolls);

        assertEquals(List.of(new OpeningRollWinnerDetermined(Colour.GREEN)),
                published(OpeningRollWinnerDetermined.class, captor));
    }

    @Test
    @DisplayName("the round order starts at the winner and follows R->G->Y->B")
    void roundOrderStartsAtWinnerAndFollowsFixedSequence() {
        // RED=2, GREEN=3, YELLOW=4, BLUE=6: Blue wins outright. Trailing 2 stalls the rest of the game.
        when(dice.roll()).thenReturn(2, 3, 4, 6, 2);

        engine.run(board, board);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());

        List<RoundOrderAnnounced> orders = published(RoundOrderAnnounced.class, captor);
        assertEquals(1, orders.size());
        assertEquals(List.of(Colour.BLUE, Colour.RED, Colour.GREEN, Colour.YELLOW), orders.get(0).order());
    }

    @Test
    @DisplayName("A-42: the game stops after 1000 rounds if nobody has finished")
    void a42_roundGuardStopsAnUnfinishedGame() {
        // RED=6 wins outright; the trailing 3 (never a six) means nobody ever leaves base.
        when(dice.roll()).thenReturn(6, 5, 4, 3, 3);

        engine.run(board, board);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());

        GameEnded ended = published(GameEnded.class, captor).get(0);
        assertTrue(ended.stoppedByRoundGuard());
        assertTrue(ended.placings().isEmpty());
    }
}
