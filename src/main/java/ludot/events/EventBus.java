package ludot.events;

import java.util.ArrayList;
import java.util.List;

/** Publishes GameEvents to every subscribed listener, in subscription order. */
public final class EventBus {

    private final List<GameEventListener> listeners = new ArrayList<>();

    public void subscribe(GameEventListener listener) {
        listeners.add(listener);
    }

    public void publish(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
