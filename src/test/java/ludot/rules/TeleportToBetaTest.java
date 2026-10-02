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
class TeleportToBetaTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private MysteryContext context;
    private final TeleportToBeta outcome = new TeleportToBeta();
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        EventBus events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, topology, events, new LandingResolver(), new FirstItemPicker());
    }

    @Test
    @DisplayName("kind() is BETA")
    void kindIsBeta() {
        assertEquals(MysteryOutcomeKind.BETA, outcome.kind());
    }

    @Test
    @DisplayName("T-11: teleports the piece onto the Beta cell")
    void teleportsPieceOntoBetaCell() {
        PieceId id = new PieceId(Colour.RED, 1);

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.betaIndex()), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: an opponent block on Beta sends the teleported piece to base")
    void opponentBlockOnBetaSendsPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.betaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.betaIndex()));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: a single opponent piece on Beta is captured")
    void singleOpponentOnBetaIsCaptured() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.betaIndex()));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
    }
}
