package ludot.moves;

import ludot.domain.PieceId;

import java.util.Optional;

/** What happened when a piece landed on a cell (currently: capture or nothing, T-8/T-16 add more). */
public record LandingResult(boolean captured, Optional<PieceId> capturedId) {
}
