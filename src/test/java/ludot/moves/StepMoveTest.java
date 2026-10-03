package ludot.moves;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.PieceMoved;
import ludot.random.Coin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StepMoveTest {

    private static final Coin UNUSED_COIN = () -> Direction.CLOCKWISE;

    @Mock
    private GameEventListener listener;

    @Mock
    private LandingHandler landingHandler;

    @Mock
    private MysteryHandler mysteryHandler;

    private BoardState board;
    private EventBus events;

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
    }

    @Test
    @DisplayName("Rule 1: moves the piece to its destination")
    void rule1_movesPieceToDestination() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new OnTrack(14), board.piece(id).position());
    }

    @Test
    @DisplayName("publishes PieceMoved with origin, destination, roll value and direction")
    void publishesPieceMovedWithFullDetail() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        ArgumentCaptor<PieceMoved> captor = ArgumentCaptor.forClass(PieceMoved.class);
        verify(listener).onEvent(captor.capture());
        PieceMoved event = captor.getValue();
        assertEquals(id, event.pieceId());
        assertEquals(new OnTrack(10), event.from());
        assertEquals(new OnTrack(14), event.to());
        assertEquals(4, event.units());
        assertEquals(Direction.CLOCKWISE, event.direction());
    }

    @Test
    @DisplayName("a non-capturing move never calls the landing handler")
    void nonCapturingMoveDoesNotCallLandingHandler() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(landingHandler);
    }

    @Test
    @DisplayName("A-46: a capturing move publishes PieceMoved, then delegates to the landing handler")
    void a46_capturingMovePublishesThenDelegates() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Position destination = new OnTrack(14);
        when(landingHandler.resolveLanding(List.of(id), destination, board, events))
                .thenReturn(new LandingResult(true, Optional.of(new PieceId(Colour.GREEN, 1))));
        Move move = new StepMove(
                id, new OnTrack(10), destination, 4, Direction.CLOCKWISE, true, false, false, false, false);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new MoveResult(true), result);
        InOrder order = inOrder(listener, landingHandler);
        order.verify(listener).onEvent(any(PieceMoved.class));
        order.verify(landingHandler).resolveLanding(List.of(id), destination, board, events);
    }

    @Test
    @DisplayName("A-08: a crossing move records an Approach crossing on the piece")
    void a08_executingCrossingMoveRecordsApproachCrossing() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(9), 1, Direction.COUNTERCLOCKWISE, false, false, false, true,
                false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(1, board.piece(id).ccwApproachCrossings());
    }

    @Test
    @DisplayName("A-08: a non-crossing move leaves the crossing count unchanged")
    void a08_executingNonCrossingMoveDoesNotRecordCrossing() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(0, board.piece(id).ccwApproachCrossings());
    }

    @Test
    @DisplayName("A-12: a step move never tosses the coin")
    void a12_stepMoveNeverTossesCoin() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false, false, false, false, false);
        Coin coin = mock(Coin.class);

        move.execute(new MoveContext(board, events, landingHandler, coin, mysteryHandler));

        verifyNoInteractions(coin);
    }

    @Test
    @DisplayName("A-29: a non-mystery move never calls the mystery handler")
    void a29_nonMysteryMoveDoesNotCallMysteryHandler() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(mysteryHandler);
    }

    @ParameterizedTest(name = "t15_a35: normal landing on {0} has no effect")
    @MethodSource("specialCells")
    void t15_a35_normalLandingOnSpecialCellHasNoEffect(String label, int index) {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(
                id, new OnTrack(10), new OnTrack(index), 4, Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(mysteryHandler);
    }

    private static Stream<Arguments> specialCells() {
        BoardTopology topology = new BoardTopology();
        return Stream.of(
                Arguments.of("Alpha", topology.alphaIndex()),
                Arguments.of("Beta", topology.betaIndex()),
                Arguments.of("Gamma", topology.gammaIndex()));
    }

    @Test
    @DisplayName("A-29: a move landing on the mystery cell triggers the mystery handler after the move is published")
    void a29_mysteryMoveTriggersMysteryHandlerAfterMoving() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Position destination = new OnTrack(14);
        when(mysteryHandler.trigger(id, board, events)).thenReturn(false);
        Move move = new StepMove(
                id, new OnTrack(10), destination, 4, Direction.CLOCKWISE, false, false, false, false, true);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        InOrder order = inOrder(listener, mysteryHandler);
        order.verify(listener).onEvent(any(PieceMoved.class));
        order.verify(mysteryHandler).trigger(id, board, events);
    }

    @Test
    @DisplayName("T-2/A-23: a capture made via the mystery teleport is reflected in the move result")
    void t2_teleportCaptureIsReflectedInMoveResult() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Position destination = new OnTrack(14);
        when(mysteryHandler.trigger(id, board, events)).thenReturn(true);
        Move move = new StepMove(
                id, new OnTrack(10), destination, 4, Direction.CLOCKWISE, false, false, false, false, true);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new MoveResult(true), result);
        verify(landingHandler, never()).resolveLanding(any(), any(), any(), any());
    }

    @Test
    @DisplayName("a move that both captures and lands on the mystery cell combines both capture results")
    void capturingMysteryMoveCombinesBothCaptureResults() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Position destination = new OnTrack(14);
        when(landingHandler.resolveLanding(List.of(id), destination, board, events))
                .thenReturn(new LandingResult(true, Optional.of(new PieceId(Colour.GREEN, 1))));
        when(mysteryHandler.trigger(id, board, events)).thenReturn(false);
        Move move = new StepMove(
                id, new OnTrack(10), destination, 4, Direction.CLOCKWISE, true, false, false, false, true);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new MoveResult(true), result);
        InOrder order = inOrder(listener, landingHandler, mysteryHandler);
        order.verify(listener).onEvent(any(PieceMoved.class));
        order.verify(landingHandler).resolveLanding(List.of(id), destination, board, events);
        order.verify(mysteryHandler).trigger(id, board, events);
    }
}
