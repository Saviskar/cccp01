package ludot.events;

import ludot.domain.PieceId;

/** Rule 2: a piece moved from base to its colour's starting square X. */
public record PieceEnteredX(PieceId pieceId, int onBoard, int inBase) implements GameEvent {
}
