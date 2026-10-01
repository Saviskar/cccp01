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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeleportToAlphaTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private EventBus events;
    private MysteryContext context;
    private final TeleportToAlpha outcome = new TeleportToAlpha();
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, topology, events, new LandingResolver());
    }

    @Test
    @DisplayName("kind() is ALPHA")
    void kindIsAlpha() {
        assertEquals(MysteryOutcomeKind.ALPHA, outcome.kind());
    }

    @Test
    @DisplayName("T-11: teleports the piece onto the Alpha cell")
    void teleportsPieceOntoAlphaCell() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.alphaIndex()), board.piece(id).position());
    }

    @Test
    @DisplayName("A-30: a successful teleport preserves the piece's direction, capture count, and ccw crossings")
    void a30_preservesDirectionCaptureCountAndCrossings() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        board.recordCapture(id);
        board.recordApproachCrossing(id);

        outcome.apply(id, context);

        assertEquals(Optional.of(Direction.COUNTERCLOCKWISE), board.piece(id).originalDirection());
        assertEquals(1, board.piece(id).captureCount());
        assertEquals(1, board.piece(id).ccwApproachCrossings());
    }

    @Test
    @DisplayName("A-31: an empty Alpha cell is a non-capturing landing")
    void emptyAlphaCellDoesNotCapture() {
        PieceId id = new PieceId(Colour.RED, 1);

        boolean captured = outcome.apply(id, context);

        assertFalse(captured);
    }

    @Test
    @DisplayName("A-31: a single opponent piece on Alpha is captured, with the T-2 bonus roll")
    void singleOpponentOnAlphaIsCaptured() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.alphaIndex()));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
        assertEquals(1, board.piece(id).captureCount());
    }

    @Test
    @DisplayName("A-31: an own piece already on Alpha forms a block instead of capturing")
    void ownPieceOnAlphaFormsBlock() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId own = new PieceId(Colour.RED, 2);
        board.moveTo(own, new OnTrack(topology.alphaIndex()));

        boolean captured = outcome.apply(id, context);

        assertFalse(captured);
        assertTrue(board.isBlock(topology.alphaIndex()));
    }

    @Test
    @DisplayName("A-31: an opponent block on Alpha sends the teleported piece to base instead")
    void opponentBlockOnAlphaSendsPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.alphaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.alphaIndex()));

        boolean captured = outcome.apply(id, context);

        assertFalse(captured);
        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("A-26/T-9: a piece sent to base by an opponent block is fully reset")
    void pieceSentToBaseIsFullyReset() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.CLOCKWISE);
        board.recordCapture(id);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.alphaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.alphaIndex()));

        outcome.apply(id, context);

        assertEquals(0, board.piece(id).captureCount());
        assertEquals(Optional.empty(), board.piece(id).originalDirection());
    }

    @Test
    @DisplayName("publishes PieceTeleported with the resolved final position")
    void publishesPieceTeleportedWithFinalPosition() {
        PieceId id = new PieceId(Colour.RED, 1);

        outcome.apply(id, context);

        ArgumentCaptor<PieceTeleported> captor = ArgumentCaptor.forClass(PieceTeleported.class);
        verify(listener).onEvent(captor.capture());
        PieceTeleported event = captor.getValue();
        assertEquals(id, event.pieceId());
        assertEquals(MysteryOutcomeKind.ALPHA, event.destination());
        assertEquals(new OnTrack(topology.alphaIndex()), event.finalPosition());
        assertFalse(event.redirectedToBase());
    }

    @Test
    @DisplayName("publishes PieceTeleported with redirectedToBase true when sent to base by an opponent block")
    void publishesPieceTeleportedWithRedirectedToBaseTrue() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.alphaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.alphaIndex()));

        outcome.apply(id, context);

        ArgumentCaptor<PieceTeleported> captor = ArgumentCaptor.forClass(PieceTeleported.class);
        verify(listener).onEvent(captor.capture());
        PieceTeleported event = captor.getValue();
        assertEquals(new InBase(), event.finalPosition());
        assertTrue(event.redirectedToBase());
    }
}
