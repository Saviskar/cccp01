package ludot.rules;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.InBase;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.PieceCaptured;
import ludot.events.PieceCountStatus;
import ludot.moves.LandingResult;
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
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class LandingResolverTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private EventBus events;
    private final LandingResolver resolver = new LandingResolver();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
    }

    @Test
    @DisplayName("Rule 6: the captured piece returns to base")
    void rule6_capturedPieceReturnsToBase() {
        PieceId capturer = new PieceId(Colour.RED, 1);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        resolver.resolveLanding(capturer, new OnTrack(5), board, events);

        assertEquals(new InBase(), board.piece(captured).position());
    }

    @Test
    @DisplayName("A-07/T-7: the capturer's capture count is incremented")
    void a07_capturerCaptureCountIncremented() {
        PieceId capturer = new PieceId(Colour.RED, 1);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        resolver.resolveLanding(capturer, new OnTrack(5), board, events);

        assertEquals(1, board.piece(capturer).captureCount());
    }

    @Test
    @DisplayName("A-46: publishes PieceCaptured, then PieceCountStatus for the captured colour")
    void a46_publishesCapturedThenCountStatusForCapturedColour() {
        PieceId capturer = new PieceId(Colour.RED, 1);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        LandingResult result = resolver.resolveLanding(capturer, new OnTrack(5), board, events);

        assertEquals(new LandingResult(true, Optional.of(captured)), result);

        InOrder order = inOrder(listener);
        ArgumentCaptor<PieceCaptured> capturedCaptor = ArgumentCaptor.forClass(PieceCaptured.class);
        order.verify(listener).onEvent(capturedCaptor.capture());
        ArgumentCaptor<PieceCountStatus> statusCaptor = ArgumentCaptor.forClass(PieceCountStatus.class);
        order.verify(listener).onEvent(statusCaptor.capture());

        assertEquals(capturer, capturedCaptor.getValue().capturerId());
        assertEquals(captured, capturedCaptor.getValue().capturedId());
        assertEquals(Colour.GREEN, statusCaptor.getValue().colour());
        assertEquals(0, statusCaptor.getValue().onBoard());
        assertEquals(4, statusCaptor.getValue().inBase());
    }
}
