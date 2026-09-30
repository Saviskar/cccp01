package ludot.events;

import ludot.domain.Colour;

/**
 * A colour's board/base piece count changed as a side effect of another
 * event (e.g. one of its pieces was captured, A-46).
 */
public record PieceCountStatus(Colour colour, int onBoard, int inBase) implements GameEvent {
}
