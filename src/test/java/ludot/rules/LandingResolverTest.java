package ludot.rules;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.Direction;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

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

        resolver.resolveLanding(List.of(capturer), new OnTrack(5), board, events);

        assertEquals(new InBase(), board.piece(captured).position());
    }

    @Test
    @DisplayName("A-07/T-7: the capturer's capture count is incremented")
    void a07_capturerCaptureCountIncremented() {
        PieceId capturer = new PieceId(Colour.RED, 1);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        resolver.resolveLanding(List.of(capturer), new OnTrack(5), board, events);

        assertEquals(1, board.piece(capturer).captureCount());
    }

    @Test
    @DisplayName("A-46: publishes PieceCaptured, then PieceCountStatus for the captured colour")
    void a46_publishesCapturedThenCountStatusForCapturedColour() {
        PieceId capturer = new PieceId(Colour.RED, 1);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        LandingResult result = resolver.resolveLanding(List.of(capturer), new OnTrack(5), board, events);

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

    @Test
    @DisplayName("T-9/A-26: a captured piece loses all of its accumulated state")
    void t9_capturedPieceLosesAllState() {
        PieceId capturer = new PieceId(Colour.RED, 1);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));
        board.assignDirection(captured, Direction.COUNTERCLOCKWISE);
        board.recordCapture(captured);
        board.recordApproachCrossing(captured);

        resolver.resolveLanding(List.of(capturer), new OnTrack(5), board, events);

        assertEquals(new InBase(), board.piece(captured).position());
        assertEquals(Optional.empty(), board.piece(captured).originalDirection());
        assertEquals(0, board.piece(captured).captureCount());
        assertEquals(0, board.piece(captured).ccwApproachCrossings());
    }

    @Test
    @DisplayName("A-19: a block capture increments every member's capture count")
    void a19_blockCaptureIncrementsEveryMembersCaptureCount() {
        PieceId capturer1 = new PieceId(Colour.RED, 1);
        PieceId capturer2 = new PieceId(Colour.RED, 2);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        resolver.resolveLanding(List.of(capturer1, capturer2), new OnTrack(5), board, events);

        assertEquals(1, board.piece(capturer1).captureCount());
        assertEquals(1, board.piece(capturer2).captureCount());
    }

    @Test
    @DisplayName("A-51: a block capture names the lowest-numbered member as capturer")
    void a51_blockCaptureNamesLowestNumberedMemberAsCapturer() {
        PieceId lowestNumbered = new PieceId(Colour.RED, 1);
        PieceId other = new PieceId(Colour.RED, 2);
        PieceId captured = new PieceId(Colour.GREEN, 1);
        board.moveTo(captured, new OnTrack(5));

        LandingResult result =
                resolver.resolveLanding(List.of(lowestNumbered, other), new OnTrack(5), board, events);

        assertEquals(new LandingResult(true, Optional.of(captured)), result);
        ArgumentCaptor<PieceCaptured> capturedCaptor = ArgumentCaptor.forClass(PieceCaptured.class);
        verify(listener).onEvent(capturedCaptor.capture());
        assertEquals(lowestNumbered, capturedCaptor.getValue().capturerId());
    }
}
