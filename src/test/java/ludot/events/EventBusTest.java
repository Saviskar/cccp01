package ludot.events;

import ludot.domain.Colour;
import ludot.domain.PieceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventBusTest {

    @Mock
    private GameEventListener listener;

    @Mock
    private GameEventListener otherListener;

    private final EventBus bus = new EventBus();

    private PiecesIntroduced sampleEvent(Colour colour) {
        return new PiecesIntroduced(colour, List.of(
                new PieceId(colour, 1),
                new PieceId(colour, 2),
                new PieceId(colour, 3),
                new PieceId(colour, 4)));
    }

    @Test
    @DisplayName("a published event reaches a subscribed listener")
    void publishedEventReachesListener() {
        bus.subscribe(listener);
        PiecesIntroduced event = sampleEvent(Colour.RED);

        bus.publish(event);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(event, captor.getValue());
    }

    @Test
    @DisplayName("all subscribed listeners receive a published event")
    void allListenersReceiveTheEvent() {
        bus.subscribe(listener);
        bus.subscribe(otherListener);
        PiecesIntroduced event = sampleEvent(Colour.GREEN);

        bus.publish(event);

        verify(listener).onEvent(event);
        verify(otherListener).onEvent(event);
    }

    @Test
    @DisplayName("a listener is notified in subscription order across multiple events")
    void listenersNotifiedInOrder() {
        bus.subscribe(listener);
        PiecesIntroduced first = sampleEvent(Colour.YELLOW);
        PiecesIntroduced second = sampleEvent(Colour.BLUE);

        bus.publish(first);
        bus.publish(second);

        InOrder order = inOrder(listener);
        order.verify(listener).onEvent(first);
        order.verify(listener).onEvent(second);
    }
}
