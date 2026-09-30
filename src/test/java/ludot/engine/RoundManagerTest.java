package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.DiceRolled;
import ludot.events.EventBus;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
import ludot.events.RoundStatusReported;
import ludot.players.FirstLegalMoveStrategy;
import ludot.random.Dice;
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

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundManagerTest {

    private static final List<Colour> ORDER = List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE);

    @Mock
    private Dice dice;

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private EventBus events;
    private Standings standings;
    private RoundManager roundManager;

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
        standings = new Standings();
        BoardTopology topology = new BoardTopology();
        // Plumbing only: neither RoundManagerTest scenario ever rolls a six, so a lambda avoids
        // an unused Mockito stub tripping strict-stubs.
        TurnController turnController = new TurnController(
                dice, () -> Direction.CLOCKWISE, new MoveGenerator(new MovementCalculator()), topology, events,
                new LandingResolver());
        roundManager = new RoundManager(turnController, events);
    }

    private List<Player> players() {
        return ORDER.stream()
                .map(colour -> new Player(colour, new FirstLegalMoveStrategy()))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("finished colours are skipped when playing the round")
    void finishedColoursAreSkipped() {
        when(dice.roll()).thenReturn(3); // non-six, nobody has a legal move yet
        standings.recordFinish(Colour.GREEN);

        roundManager.playRound(players(), board, standings, board);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        List<Colour> rolledFor = captor.getAllValues().stream()
                .filter(DiceRolled.class::isInstance)
                .map(event -> ((DiceRolled) event).colour())
                .toList();
        assertEquals(List.of(Colour.RED, Colour.YELLOW, Colour.BLUE), rolledFor);
    }

    @Test
    @DisplayName("every colour gets exactly one RoundStatusReported per round, including finished ones")
    void everyColourGetsOneRoundStatusReportedPerRound() {
        when(dice.roll()).thenReturn(3);
        standings.recordFinish(Colour.GREEN);
        board.moveTo(new PieceId(Colour.GREEN, 1), new AtHome());
        PieceId redOnTrack = new PieceId(Colour.RED, 1);
        board.moveTo(redOnTrack, new OnTrack(5));
        board.assignDirection(redOnTrack, Direction.CLOCKWISE);

        roundManager.playRound(players(), board, standings, board);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        List<Colour> reported = captor.getAllValues().stream()
                .filter(RoundStatusReported.class::isInstance)
                .map(event -> ((RoundStatusReported) event).colour())
                .toList();
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE), reported);
    }
}
