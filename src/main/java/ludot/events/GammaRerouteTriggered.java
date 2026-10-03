package ludot.events;

import ludot.domain.PieceId;

/**
 * T-14/A-34/A-58: a counterclockwise piece teleported to Gamma is rerouted on to Beta,
 * where the Beta effect (A-33) applies as part of the same mystery-cell chain.
 */
public record GammaRerouteTriggered(PieceId pieceId) implements GameEvent {
}
