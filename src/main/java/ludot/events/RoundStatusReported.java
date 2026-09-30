package ludot.events;

import ludot.domain.Colour;

import java.util.List;

/** Published once per colour at the end of every round: counts plus every piece's location. */
public record RoundStatusReported(Colour colour, int onBoard, int inBase, List<PieceLocation> locations)
        implements GameEvent {

    public RoundStatusReported {
        locations = List.copyOf(locations);
    }
}
