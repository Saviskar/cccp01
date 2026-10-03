package ludot.random;

import java.util.List;

/**
 * Hand-written test fake (not a mock, per CLAUDE.md): records the exact list it was last asked
 * to pick from, and returns the option at a configurable index (default 0). {@link RandomPicker
 * #pick} is a generic method, so a plain lambda cannot target it -- this fake lets a test assert
 * both the candidate pool {@code BlueStrategy} (A-39) builds and that the chosen move is whatever
 * the picker returns, not some hardcoded figure.
 */
public final class RecordingPicker implements RandomPicker {

    private final int index;
    private List<?> lastOptions;

    public RecordingPicker() {
        this(0);
    }

    public RecordingPicker(int index) {
        this.index = index;
    }

    @Override
    public <T> T pick(List<T> options) {
        lastOptions = options;
        return options.get(index);
    }

    public List<?> lastOptions() {
        return lastOptions;
    }
}
