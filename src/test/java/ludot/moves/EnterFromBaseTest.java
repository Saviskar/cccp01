package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.PieceDirectionAssigned;
import ludot.events.PieceEnteredX;
import ludot.random.Coin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterFromBaseTest {

    private static final int RED_X = 26;

    @Mock
    private GameEventListener listener;

    @Mock
    private LandingHandler landingHandler;

    @Mock
    private Coin coin;

    private BoardState board;
    private EventBus events;

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
    }

    @Test
    @DisplayName("Rule 2: moves the piece from base to X")
    void rule2_movesPieceFromBaseToX() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.CLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        assertEquals(new OnTrack(RED_X), board.piece(id).position());
    }

    @Test
    @DisplayName("Rule 8/A-12: heads assigns the clockwise direction")
    void rule8_a12_headsAssignsClockwise() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.CLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        assertEquals(Optional.of(Direction.CLOCKWISE), board.piece(id).originalDirection());
    }

    @Test
    @DisplayName("A-12: tails assigns the counterclockwise direction")
    void a12_tailsAssignsCounterclockwise() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.COUNTERCLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        assertEquals(Optional.of(Direction.COUNTERCLOCKWISE), board.piece(id).originalDirection());
    }

    @Test
    @DisplayName("A-12: the coin is tossed exactly once")
    void a12_tossesCoinExactlyOnce() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.CLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        verify(coin).toss();
    }

    @Test
    @DisplayName("publishes PieceEnteredX with the mover's updated board/base counts")
    void publishesPieceEnteredXWithUpdatedCounts() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.CLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        ArgumentCaptor<PieceEnteredX> captor = ArgumentCaptor.forClass(PieceEnteredX.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(id, captor.getValue().pieceId());
        assertEquals(1, captor.getValue().onBoard());
        assertEquals(3, captor.getValue().inBase());
    }

    @Test
    @DisplayName("A-12: publishes PieceDirectionAssigned after PieceEnteredX")
    void a12_publishesPieceDirectionAssignedAfterPieceEnteredX() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.COUNTERCLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        ArgumentCaptor<PieceDirectionAssigned> captor = ArgumentCaptor.forClass(PieceDirectionAssigned.class);
        InOrder order = inOrder(listener);
        order.verify(listener).onEvent(any(PieceEnteredX.class));
        order.verify(listener).onEvent(captor.capture());
        assertEquals(id, captor.getValue().pieceId());
        assertEquals(Direction.COUNTERCLOCKWISE, captor.getValue().direction());
    }

    @Test
    @DisplayName("a non-capturing entry never calls the landing handler")
    void nonCapturingEntryDoesNotCallLandingHandler() {
        PieceId id = new PieceId(Colour.RED, 1);
        Move move = new EnterFromBase(id, new OnTrack(RED_X), false, false);
        when(coin.toss()).thenReturn(Direction.CLOCKWISE);

        move.execute(new MoveContext(board, events, landingHandler, coin));

        verifyNoInteractions(landingHandler);
    }

    @Test
    @DisplayName("A-46: a capturing entry publishes PieceEnteredX, then delegates to the landing handler")
    void a46_capturingEntryPublishesThenDelegates() {
        PieceId id = new PieceId(Colour.RED, 1);
        Position destination = new OnTrack(RED_X);
        when(landingHandler.resolveLanding(id, destination, board, events))
                .thenReturn(new LandingResult(true, Optional.of(new PieceId(Colour.GREEN, 1))));
        when(coin.toss()).thenReturn(Direction.CLOCKWISE);
        Move move = new EnterFromBase(id, destination, true, false);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler, coin));

        assertEquals(new MoveResult(true), result);
        InOrder order = inOrder(listener, landingHandler);
        order.verify(listener).onEvent(any(PieceEnteredX.class));
        order.verify(landingHandler).resolveLanding(id, destination, board, events);
    }
}
