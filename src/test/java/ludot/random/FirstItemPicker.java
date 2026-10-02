package ludot.random;

import java.util.List;

/**
 * Hand-written test fake (not a mock, per CLAUDE.md): always picks the first
 * option. {@link RandomPicker#pick} is a generic method, so a plain lambda
 * expression cannot target it (Java cannot infer a type parameter for a
 * lambda's own method); this fake is the equivalent of {@code FirstLegalMoveStrategy}
 * for the places a test needs some deterministic {@link RandomPicker} but
 * doesn't care which option comes back.
 */
public final class FirstItemPicker implements RandomPicker {

    @Override
    public <T> T pick(List<T> options) {
        return options.get(0);
    }
}
