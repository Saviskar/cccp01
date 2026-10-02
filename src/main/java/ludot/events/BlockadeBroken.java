package ludot.events;

import ludot.domain.Colour;
import ludot.domain.PieceId;

import java.util.List;

/**
 * T-6/A-22 (A-44): a block breaks after a third consecutive six. Published once per block, before
 * its leaving members move. Ordinarily {@code staying} holds exactly one member (A-22); A-57
 * widens it to a list, since every Beta-restricted member of a block stays behind.
 */
public record BlockadeBroken(Colour colour, int cell, List<PieceId> staying, List<PieceId> leaving, int unitsEach)
        implements GameEvent {

    public BlockadeBroken {
        staying = List.copyOf(staying);
        leaving = List.copyOf(leaving);
    }
}
