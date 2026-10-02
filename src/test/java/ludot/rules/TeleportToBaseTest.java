package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.PieceTeleported;
import ludot.random.FirstItemPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeleportToBaseTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private MysteryContext context;
    private final TeleportToBase outcome = new TeleportToBase();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        EventBus events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, new BoardTopology(), events, new LandingResolver(), new FirstItemPicker());
    }

    @Test
    @DisplayName("kind() is BASE")
    void kindIsBase() {
        assertEquals(MysteryOutcomeKind.BASE, outcome.kind());
    }

    @Test
    @DisplayName("A-26/T-9: teleporting to base fully resets the piece")
    void a26_teleportingToBaseFullyResetsPiece() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.recordCapture(id);

        boolean captured = outcome.apply(id, context);

        assertFalse(captured);
        assertEquals(new InBase(), board.piece(id).position());
        assertEquals(0, board.piece(id).captureCount());
        assertEquals(Optional.empty(), board.piece(id).originalDirection());
    }

    @Test
    @DisplayName("publishes PieceTeleported to Base, not redirected")
    void publishesPieceTeleportedToBase() {
        PieceId id = new PieceId(Colour.RED, 1);

        outcome.apply(id, context);

        ArgumentCaptor<PieceTeleported> captor = ArgumentCaptor.forClass(PieceTeleported.class);
        verify(listener).onEvent(captor.capture());
        PieceTeleported event = captor.getValue();
        assertEquals(id, event.pieceId());
        assertEquals(MysteryOutcomeKind.BASE, event.destination());
        assertEquals(new InBase(), event.finalPosition());
        assertFalse(event.redirectedToBase());
    }
}
