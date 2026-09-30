package ludot.events;

import ludot.domain.Colour;

import java.util.List;

/** Published once, when the game loop exits: A-41 final placings and whether A-42's round guard caused it. */
public record GameEnded(List<Colour> placings, boolean stoppedByRoundGuard) implements GameEvent {

    public GameEnded {
        placings = List.copyOf(placings);
    }
}
