package ludot.events;

import ludot.domain.Colour;

import java.util.List;

/**
 * Published once, when the game loop exits: A-41 final placings and whether A-42's round guard
 * caused it. {@code notFinished} lists every colour that never reached Home, in the fixed turn
 * order (A-04); it is only used when {@code stoppedByRoundGuard} is true (A-61).
 */
public record GameEnded(List<Colour> placings, boolean stoppedByRoundGuard, List<Colour> notFinished)
        implements GameEvent {

    public GameEnded {
        placings = List.copyOf(placings);
        notFinished = List.copyOf(notFinished);
    }
}
