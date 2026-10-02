package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.Energised;
import ludot.domain.NoEffect;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.DiceRolled;
import ludot.events.EventBus;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
import ludot.events.MysteryCellStatusReported;
import ludot.events.MysterySpawned;
import ludot.events.RoundStatusReported;
import ludot.players.FirstLegalMoveStrategy;
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

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
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

    @Mock
    private RandomPicker randomPicker;

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
        // an unused Mockito stub tripping strict-stubs. Likewise, no scenario here reaches a
        // mystery-cell trigger, so this handler is never called.
        TurnController turnController = new TurnController(
                dice, () -> Direction.CLOCKWISE, new MoveGenerator(new MovementCalculator()), topology, events,
                new LandingResolver(), new BlockBreakPlanner(), (pieceId, b, e) -> false);
        roundManager = new RoundManager(turnController, events, randomPicker);
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

    @Test
    @DisplayName("no mystery-cell events are published while no piece has reached the standard track")
    void noMysteryEventsPublishedWithoutAnyPieceOnTrack() {
        when(dice.roll()).thenReturn(3); // non-six, nobody leaves base

        roundManager.playRound(players(), board, standings, board);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertTrue(captor.getAllValues().stream().noneMatch(MysterySpawned.class::isInstance));
        assertTrue(captor.getAllValues().stream().noneMatch(MysteryCellStatusReported.class::isInstance));
    }

    @Test
    @DisplayName("A-28: the mystery cell spawns and is reported once the timing window elapses")
    void a28_mysteryCellSpawnsAndIsReportedAfterTimingWindow() {
        when(dice.roll()).thenReturn(3); // non-six, nobody moves once on the track either
        when(randomPicker.pick(anyList())).thenReturn(7);
        PieceId redOnTrack = new PieceId(Colour.RED, 1);
        board.moveTo(redOnTrack, new OnTrack(5));
        board.assignDirection(redOnTrack, Direction.CLOCKWISE);

        roundManager.playRound(players(), board, standings, board); // starts the A-28 timer
        roundManager.playRound(players(), board, standings, board); // one round-end to go
        roundManager.playRound(players(), board, standings, board); // spawns now

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertEquals(List.of(new MysterySpawned(7)),
                captor.getAllValues().stream().filter(MysterySpawned.class::isInstance).toList());
        assertEquals(new MysteryCellStatusReported(7, 4),
                captor.getAllValues().stream()
                        .filter(MysteryCellStatusReported.class::isInstance)
                        .reduce((first, second) -> second) // the last one published
                        .orElseThrow());
    }

    @Test
    @DisplayName("A-28: the round after spawning reports one fewer round remaining, with no second spawn")
    void a28_nextRoundReportsOneFewerRoundRemaining() {
        when(dice.roll()).thenReturn(3);
        when(randomPicker.pick(anyList())).thenReturn(7);
        PieceId redOnTrack = new PieceId(Colour.RED, 1);
        board.moveTo(redOnTrack, new OnTrack(5));
        board.assignDirection(redOnTrack, Direction.CLOCKWISE);
        roundManager.playRound(players(), board, standings, board);
        roundManager.playRound(players(), board, standings, board);
        roundManager.playRound(players(), board, standings, board); // spawns, 4 rounds remaining

        roundManager.playRound(players(), board, standings, board); // 3 rounds remaining

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertEquals(1, captor.getAllValues().stream().filter(MysterySpawned.class::isInstance).count());
        assertEquals(new MysteryCellStatusReported(7, 3),
                captor.getAllValues().stream()
                        .filter(MysteryCellStatusReported.class::isInstance)
                        .reduce((first, second) -> second)
                        .orElseThrow());
    }

    @Test
    @DisplayName("T-12/A-32: an effect one tick from expiry expires after exactly one playRound")
    void t12_a32_effectOneTickFromExpiryExpiresAfterOneRound() {
        when(dice.roll()).thenReturn(3); // non-six, no legal moves generated either way
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(5));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Energised(1));

        roundManager.playRound(players(), board, standings, board);

        assertEquals(new NoEffect(), board.piece(id).effect());
    }

    @Test
    @DisplayName("T-12/A-32: a fresh effect survives 4 rounds and expires only after the 5th")
    void t12_a32_freshEffectExpiresAfterFifthRound() {
        when(dice.roll()).thenReturn(3);
        when(randomPicker.pick(anyList())).thenReturn(7); // A-28's mystery timer spawns within these 5 rounds
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(5));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Energised());

        for (int round = 1; round <= 4; round++) {
            roundManager.playRound(players(), board, standings, board);
            assertTrue(board.piece(id).effect() instanceof Energised, "still active after round " + round);
        }
        roundManager.playRound(players(), board, standings, board); // 5th round-end: expires

        assertEquals(new NoEffect(), board.piece(id).effect());
    }
}
