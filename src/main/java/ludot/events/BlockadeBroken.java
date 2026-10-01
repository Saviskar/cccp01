package ludot.events;

import ludot.domain.Colour;
import ludot.domain.PieceId;

import java.util.List;

/**
 * T-6/A-22 (A-44): a block breaks after a third consecutive six. Published once per block, before
 * its leaving members move.
 */
public record BlockadeBroken(Colour colour, int cell, PieceId staying, List<PieceId> leaving, int unitsEach)
        implements GameEvent {

    public BlockadeBroken {
        leaving = List.copyOf(leaving);
    }
}
