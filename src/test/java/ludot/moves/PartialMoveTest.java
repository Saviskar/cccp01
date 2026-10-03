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
import ludot.events.PieceBlocked;
import ludot.events.PiecePartiallyMoved;
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
class PartialMoveTest {

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
    @DisplayName("A-16: moves the piece to the cell before the block")
    void a16_movesPieceToStoppingCell() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        PieceId blocker = new PieceId(Colour.GREEN, 1);
        Move move = new PartialMove(
                id, new OnTrack(0), new OnTrack(3), new OnTrack(6), blocker, 6, 3, Direction.CLOCKWISE, false, false,
                false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new OnTrack(3), board.piece(id).position());
    }

    @Test
    @DisplayName("A-48: publishes PieceBlocked then PiecePartiallyMoved, in order")
    void a48_publishesBlockedThenPartiallyMovedInOrder() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        PieceId blocker = new PieceId(Colour.GREEN, 1);
        Move move = new PartialMove(
                id, new OnTrack(0), new OnTrack(3), new OnTrack(6), blocker, 6, 3, Direction.CLOCKWISE, false, false,
                false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        InOrder order = inOrder(listener);
        ArgumentCaptor<PieceBlocked> blockedCaptor = ArgumentCaptor.forClass(PieceBlocked.class);
        ArgumentCaptor<PiecePartiallyMoved> movedCaptor = ArgumentCaptor.forClass(PiecePartiallyMoved.class);
        order.verify(listener).onEvent(blockedCaptor.capture());
        order.verify(listener).onEvent(movedCaptor.capture());

        PieceBlocked blocked = blockedCaptor.getValue();
        assertEquals(id, blocked.pieceId());
        assertEquals(new OnTrack(0), blocked.from());
        assertEquals(new OnTrack(6), blocked.intendedDestination());
        assertEquals(blocker, blocked.blockingPieceId());

        PiecePartiallyMoved moved = movedCaptor.getValue();
        assertEquals(id, moved.pieceId());
        assertEquals(new OnTrack(0), moved.from());
        assertEquals(new OnTrack(3), moved.to());
        assertEquals(3, moved.cellsMoved());
        assertEquals(Direction.CLOCKWISE, moved.direction());
    }

    @Test
    @DisplayName("A-08: a crossing partial move records an Approach crossing on the piece")
    void a08_crossingPartialMoveRecordsApproachCrossing() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        Move move = new PartialMove(
                id, new OnTrack(10), new OnTrack(9), new OnTrack(6), new PieceId(Colour.GREEN, 1), 4, 1,
                Direction.COUNTERCLOCKWISE, false, false, false, true, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(1, board.piece(id).ccwApproachCrossings());
    }

    @Test
    @DisplayName("a non-capturing partial move never calls the landing handler")
    void nonCapturingPartialMoveDoesNotCallLandingHandler() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Move move = new PartialMove(
                id, new OnTrack(0), new OnTrack(3), new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(landingHandler);
    }

    @Test
    @DisplayName("A-46: a capturing partial move publishes the movement facts, then delegates to the landing handler")
    void a46_capturingPartialMovePublishesThenDelegates() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Position destination = new OnTrack(3);
        when(landingHandler.resolveLanding(List.of(id), destination, board, events))
                .thenReturn(new LandingResult(true, Optional.of(new PieceId(Colour.BLUE, 1))));
        Move move = new PartialMove(
                id, new OnTrack(0), destination, new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, true, false, false, false, false);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new MoveResult(true), result);
        InOrder order = inOrder(listener, landingHandler);
        order.verify(listener).onEvent(any(PieceBlocked.class));
        order.verify(listener).onEvent(any(PiecePartiallyMoved.class));
        order.verify(landingHandler).resolveLanding(List.of(id), destination, board, events);
    }

    @Test
    @DisplayName("A-12: a partial move never tosses the coin")
    void a12_partialMoveNeverTossesCoin() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Move move = new PartialMove(
                id, new OnTrack(0), new OnTrack(3), new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, false, false, false, false, false);
        Coin coin = mock(Coin.class);

        move.execute(new MoveContext(board, events, landingHandler, coin, mysteryHandler));

        verifyNoInteractions(coin);
    }

    @Test
    @DisplayName("A-29: a non-mystery partial move never calls the mystery handler")
    void a29_nonMysteryPartialMoveDoesNotCallMysteryHandler() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Move move = new PartialMove(
                id, new OnTrack(0), new OnTrack(3), new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, false, false, false, false, false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(mysteryHandler);
    }

    @Test
    @DisplayName("A-29: a partial move landing on the mystery cell triggers the mystery handler after moving")
    void a29_mysteryPartialMoveTriggersMysteryHandlerAfterMoving() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Position destination = new OnTrack(3);
        when(mysteryHandler.trigger(id, board, events)).thenReturn(false);
        Move move = new PartialMove(
                id, new OnTrack(0), destination, new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, false, false, false, false, true);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        InOrder order = inOrder(listener, mysteryHandler);
        order.verify(listener).onEvent(any(PiecePartiallyMoved.class));
        order.verify(mysteryHandler).trigger(id, board, events);
    }

    @ParameterizedTest(name = "t15_a35: normal partial-move landing on {0} has no effect")
    @MethodSource("specialCells")
    void t15_a35_normalLandingOnSpecialCellHasNoEffect(String label, int index) {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Move move = new PartialMove(
                id, new OnTrack(0), new OnTrack(index), new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, false, false, false, false, false);

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
    @DisplayName("A-23: a capture made via the mystery teleport still grants the bonus-roll result")
    void a23_teleportCaptureIsReflectedInMoveResult() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(0));
        Position destination = new OnTrack(3);
        when(mysteryHandler.trigger(id, board, events)).thenReturn(true);
        Move move = new PartialMove(
                id, new OnTrack(0), destination, new OnTrack(6), new PieceId(Colour.GREEN, 1), 6, 3,
                Direction.CLOCKWISE, false, false, false, false, true);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new MoveResult(true), result);
        verify(landingHandler, never()).resolveLanding(any(), any(), any(), any());
    }
}
