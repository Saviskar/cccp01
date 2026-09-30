package ludot.events;

import ludot.domain.Colour;

import java.util.List;

/** Published once, right after the opening roll: the fixed R->G->Y->B order starting at the winner. */
public record RoundOrderAnnounced(List<Colour> order) implements GameEvent {

    public RoundOrderAnnounced {
        order = List.copyOf(order);
    }
}
