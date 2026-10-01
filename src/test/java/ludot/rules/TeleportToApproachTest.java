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
class TeleportToApproachTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private MysteryContext context;
    private final TeleportToApproach outcome = new TeleportToApproach();
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        EventBus events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, topology, events, new LandingResolver());
    }

    @Test
    @DisplayName("kind() is APPROACH")
    void kindIsApproach() {
        assertEquals(MysteryOutcomeKind.APPROACH, outcome.kind());
    }

    @Test
    @DisplayName("T-11/A-02: teleports the piece onto its own colour's Approach cell")
    void teleportsPieceOntoOwnColourApproachCell() {
        PieceId id = new PieceId(Colour.BLUE, 1);

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.approachIndex(Colour.BLUE)), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: an opponent block on Approach sends the teleported piece to base")
    void opponentBlockOnApproachSendsPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        int approach = topology.approachIndex(Colour.RED);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(approach));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(approach));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: a single opponent piece on Approach is captured")
    void singleOpponentOnApproachIsCaptured() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.approachIndex(Colour.RED)));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
    }
}
