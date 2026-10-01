package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Colour;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TeleportToXTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private MysteryContext context;
    private final TeleportToX outcome = new TeleportToX();
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        EventBus events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, topology, events, new LandingResolver());
    }

    @Test
    @DisplayName("kind() is X")
    void kindIsX() {
        assertEquals(MysteryOutcomeKind.X, outcome.kind());
    }

    @Test
    @DisplayName("T-11/A-02: teleports the piece onto its own colour's X cell")
    void teleportsPieceOntoOwnColourXCell() {
        PieceId id = new PieceId(Colour.GREEN, 1);

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.xIndex(Colour.GREEN)), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: an opponent block on X sends the teleported piece to base")
    void opponentBlockOnXSendsPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        int x = topology.xIndex(Colour.RED);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(x));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(x));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: a single opponent piece on X is captured")
    void singleOpponentOnXIsCaptured() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.xIndex(Colour.RED)));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
    }
}
