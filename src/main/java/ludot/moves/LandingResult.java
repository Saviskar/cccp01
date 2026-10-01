package ludot.moves;

import ludot.domain.PieceId;

import java.util.Optional;

/**
 * What happened when a piece landed on a cell (currently: capture or nothing, T-16 adds more).
 * {@code capturedId} is the lowest-numbered captured piece (A-53): a block-vs-block capture
 * (T-8/A-20) can return more than one opponent piece to base in a single landing, but nothing
 * outside tests reads this field for identity, so it is not widened to a list.
 */
public record LandingResult(boolean captured, Optional<PieceId> capturedId) {
}
