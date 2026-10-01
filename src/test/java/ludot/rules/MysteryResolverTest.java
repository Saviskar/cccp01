package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Colour;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.MysteryCellTriggered;
import ludot.random.RandomPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MysteryResolverTest {

    @Mock
    private RandomPicker picker;

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private EventBus events;
    private MysteryResolver resolver;
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
        resolver = new MysteryResolver(new MysteryOutcomeFactory(), picker, topology, new LandingResolver());
    }

    @Test
    @DisplayName("T-11: picks an outcome via the injected RandomPicker and applies it")
    void t11_picksOutcomeViaPickerAndAppliesIt() {
        PieceId id = new PieceId(Colour.RED, 1);
        when(picker.pick(anyList())).thenAnswer(inv -> {
            List<MysteryOutcome> options = inv.getArgument(0);
            return options.stream().filter(o -> o.kind() == MysteryOutcomeKind.X).findFirst().orElseThrow();
        });

        resolver.trigger(id, board, events);

        assertEquals(new OnTrack(topology.xIndex(Colour.RED)), board.piece(id).position());
    }

    @Test
    @DisplayName("A-29: publishes MysteryCellTriggered naming the drawn outcome, before resolving the landing")
    void a29_publishesMysteryCellTriggeredBeforeLanding() {
        PieceId id = new PieceId(Colour.RED, 1);
        when(picker.pick(anyList())).thenAnswer(inv -> {
            List<MysteryOutcome> options = inv.getArgument(0);
            return options.stream().filter(o -> o.kind() == MysteryOutcomeKind.ALPHA).findFirst().orElseThrow();
        });

        resolver.trigger(id, board, events);

        InOrder order = inOrder(listener);
        ArgumentCaptor<MysteryCellTriggered> captor = ArgumentCaptor.forClass(MysteryCellTriggered.class);
        order.verify(listener).onEvent(captor.capture());
        order.verify(listener).onEvent(any());
        assertEquals(id, captor.getValue().pieceId());
        assertEquals(MysteryOutcomeKind.ALPHA, captor.getValue().destination());
    }

    @Test
    @DisplayName("T-2/A-23: returns true when the resolved teleport captures an opponent piece")
    void t2_returnsTrueWhenTeleportCaptures() {
        PieceId id = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.xIndex(Colour.RED)));
        when(picker.pick(anyList())).thenAnswer(inv -> {
            List<MysteryOutcome> options = inv.getArgument(0);
            return options.stream().filter(o -> o.kind() == MysteryOutcomeKind.X).findFirst().orElseThrow();
        });

        boolean captured = resolver.trigger(id, board, events);

        assertTrue(captured);
    }
}
