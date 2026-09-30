package ludot.engine;

import ludot.domain.Colour;
import ludot.players.PlayerStrategy;

/** A colour paired with its behaviour: composition over a per-colour subclass (DESIGN.md §6). */
public record Player(Colour colour, PlayerStrategy strategy) {
}
