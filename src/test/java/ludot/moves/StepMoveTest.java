package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.PieceMoved;
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
class StepMoveTest {

    @Mock
    private GameEventListener listener;

    @Mock
    private LandingHandler landingHandler;

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
        Move move = new StepMove(id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false);

        move.execute(new MoveContext(board, events, landingHandler));

        assertEquals(new OnTrack(14), board.piece(id).position());
    }

    @Test
    @DisplayName("publishes PieceMoved with origin, destination, roll value and direction")
    void publishesPieceMovedWithFullDetail() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false);

        move.execute(new MoveContext(board, events, landingHandler));

        ArgumentCaptor<PieceMoved> captor = ArgumentCaptor.forClass(PieceMoved.class);
        verify(listener).onEvent(captor.capture());
        PieceMoved event = captor.getValue();
        assertEquals(id, event.pieceId());
        assertEquals(new OnTrack(10), event.from());
        assertEquals(new OnTrack(14), event.to());
        assertEquals(4, event.rollValue());
        assertEquals(Direction.CLOCKWISE, event.direction());
    }

    @Test
    @DisplayName("a non-capturing move never calls the landing handler")
    void nonCapturingMoveDoesNotCallLandingHandler() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Move move = new StepMove(id, new OnTrack(10), new OnTrack(14), 4, Direction.CLOCKWISE, false);

        move.execute(new MoveContext(board, events, landingHandler));

        verifyNoInteractions(landingHandler);
    }

    @Test
    @DisplayName("A-46: a capturing move publishes PieceMoved, then delegates to the landing handler")
    void a46_capturingMovePublishesThenDelegates() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        Position destination = new OnTrack(14);
        when(landingHandler.resolveLanding(id, destination, board, events))
                .thenReturn(new LandingResult(true, Optional.of(new PieceId(Colour.GREEN, 1))));
        Move move = new StepMove(id, new OnTrack(10), destination, 4, Direction.CLOCKWISE, true);

        MoveResult result = move.execute(new MoveContext(board, events, landingHandler));

        assertEquals(new MoveResult(true), result);
        InOrder order = inOrder(listener, landingHandler);
        order.verify(listener).onEvent(any(PieceMoved.class));
        order.verify(landingHandler).resolveLanding(id, destination, board, events);
    }
}
