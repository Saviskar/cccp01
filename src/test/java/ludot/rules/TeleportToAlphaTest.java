package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.AlphaEffectKind;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.Energised;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.NoEffect;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Sick;
import ludot.events.AlphaEffectAssigned;
import ludot.events.EventBus;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
import ludot.events.PieceTeleported;
import ludot.random.FirstItemPicker;
import ludot.random.RandomPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeleportToAlphaTest {

    // T-12: TeleportToAlpha always builds List.of(ENERGISED, SICK), so a picker
    // returning the last item always draws SICK.
    private static final class LastItemPicker implements RandomPicker {
        @Override
        public <T> T pick(List<T> options) {
            return options.get(options.size() - 1);
        }
    }

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
        // FirstItemPicker draws ENERGISED by default (first of List.of(ENERGISED, SICK));
        // tests needing SICK build their own context with a LastItemPicker instead.
        context = new MysteryContext(board, topology, events, new LandingResolver(), new FirstItemPicker());
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

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        PieceTeleported event = captor.getAllValues().stream()
                .filter(PieceTeleported.class::isInstance)
                .map(PieceTeleported.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(id, event.pieceId());
        assertEquals(MysteryOutcomeKind.ALPHA, event.destination());
        assertEquals(new OnTrack(topology.alphaIndex()), event.finalPosition());
        assertFalse(event.redirectedToBase());
    }

    @Test
    @DisplayName("t12_a32: landing on Alpha and drawing ENERGISED applies the Energised effect")
    void t12_a32_energisedRollAppliesEnergisedEffect() {
        PieceId id = new PieceId(Colour.RED, 1);

        outcome.apply(id, context); // FirstItemPicker -> ENERGISED

        assertEquals(new Energised(), board.piece(id).effect());
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        AlphaEffectAssigned event = captor.getAllValues().stream()
                .filter(AlphaEffectAssigned.class::isInstance)
                .map(AlphaEffectAssigned.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(id, event.pieceId());
        assertEquals(AlphaEffectKind.ENERGISED, event.kind());
    }

    @Test
    @DisplayName("t12_a32: landing on Alpha and drawing SICK applies the Sick effect")
    void t12_a32_sickRollAppliesSickEffect() {
        PieceId id = new PieceId(Colour.RED, 1);
        MysteryContext sickContext =
                new MysteryContext(board, topology, events, new LandingResolver(), new LastItemPicker());

        outcome.apply(id, sickContext);

        assertEquals(new Sick(), board.piece(id).effect());
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        AlphaEffectAssigned event = captor.getAllValues().stream()
                .filter(AlphaEffectAssigned.class::isInstance)
                .map(AlphaEffectAssigned.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(id, event.pieceId());
        assertEquals(AlphaEffectKind.SICK, event.kind());
    }

    @Test
    @DisplayName("t12_a45: a piece with an existing, partially-expired effect gets a fresh full-duration effect")
    void t12_a45_alphaOnAlreadyAffectedPieceRestartsDuration() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.applyEffect(id, new Energised(2)); // partway through an earlier countdown

        outcome.apply(id, context); // FirstItemPicker -> ENERGISED again

        assertEquals(new Energised(), board.piece(id).effect()); // fresh, full-duration instance
    }

    @Test
    @DisplayName("A-31/T-12: redirected to base by an opponent block never rolls an effect")
    void redirectedToBaseNeverRollsEffect() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.alphaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.alphaIndex()));

        outcome.apply(id, context);

        assertEquals(new NoEffect(), board.piece(id).effect());
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertTrue(captor.getAllValues().stream().noneMatch(AlphaEffectAssigned.class::isInstance));
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
        assertEquals(Optional.of(Colour.GREEN), event.blockingColour()); // A-60
    }
}
