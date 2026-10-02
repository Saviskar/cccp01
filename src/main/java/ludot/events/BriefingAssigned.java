package ludot.events;

import ludot.domain.PieceId;

/** T-13/A-33: a piece that landed on Beta attends a briefing and cannot move for 4 rounds. */
public record BriefingAssigned(PieceId pieceId) implements GameEvent {
}
