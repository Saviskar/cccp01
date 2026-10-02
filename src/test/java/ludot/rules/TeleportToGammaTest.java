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
import ludot.random.FirstItemPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TeleportToGammaTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private MysteryContext context;
    private final TeleportToGamma outcome = new TeleportToGamma();
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        EventBus events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, topology, events, new LandingResolver(), new FirstItemPicker());
    }

    @Test
    @DisplayName("kind() is GAMMA")
    void kindIsGamma() {
        assertEquals(MysteryOutcomeKind.GAMMA, outcome.kind());
    }

    @Test
    @DisplayName("T-11: teleports the piece onto the Gamma cell; T-14's direction change/reroute is phase 4k")
    void teleportsPieceOntoGammaCell() {
        PieceId id = new PieceId(Colour.RED, 1);

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.gammaIndex()), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: an opponent block on Gamma sends the teleported piece to base")
    void opponentBlockOnGammaSendsPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.gammaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.gammaIndex()));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: a single opponent piece on Gamma is captured")
    void singleOpponentOnGammaIsCaptured() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.gammaIndex()));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
    }
}
