package ludot.random;

import java.util.List;

/** Injected uniform random choice among a list of options. */
public interface RandomPicker {
    <T> T pick(List<T> options);
}
