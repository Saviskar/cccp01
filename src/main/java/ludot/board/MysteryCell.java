package ludot.board;

import ludot.random.RandomPicker;

import java.util.List;
import java.util.Optional;

/**
 * T-10's spawn timing (A-28): when the mystery cell first appears, how long it
 * stays, and where it respawns. Deliberately independent of {@link BoardState}
 * so it can be unit tested with plain inputs; {@code BoardState} supplies the
 * track-occupancy facts this needs via {@link BoardState#tickMysteryCell}.
 */
final class MysteryCell {

    // A-28: the first spawn happens at the end of the 2nd full round after the
    // round-end at which the timer starts.
    private static final int SPAWN_DELAY_ROUNDS = 2;

    // A-28: the mystery cell stays in place for 4 rounds before it moves again.
    private static final int SPAWN_DURATION_ROUNDS = 4;

    private Optional<Integer> location = Optional.empty();
    private Optional<Integer> previousLocation = Optional.empty();
    private int roundsRemaining;
    private Optional<Integer> spawnCountdown = Optional.empty();

    public Optional<Integer> location() {
        return location;
    }

    /**
     * Called once per round-end. {@code emptyTrackCells} must reflect the board's
     * occupancy at the moment of the call (A-28: "empty at spawn time").
     */
    public Optional<MysteryCellTick> onRoundEnd(
            boolean anyPieceOnTrack, List<Integer> emptyTrackCells, RandomPicker picker) {
        if (location.isPresent()) {
            return Optional.of(tickActive(emptyTrackCells, picker));
        }
        return tickWaiting(anyPieceOnTrack, emptyTrackCells, picker);
    }

    private MysteryCellTick tickActive(List<Integer> emptyTrackCells, RandomPicker picker) {
        roundsRemaining--;
        if (roundsRemaining == 0) {
            previousLocation = location;
            spawn(emptyTrackCells, picker); // A-28: stays 4 rounds, then immediately moves
            return new MysteryCellTick(location.orElseThrow(), roundsRemaining, true);
        }
        return new MysteryCellTick(location.orElseThrow(), roundsRemaining, false);
    }

    private Optional<MysteryCellTick> tickWaiting(
            boolean anyPieceOnTrack, List<Integer> emptyTrackCells, RandomPicker picker) {
        if (spawnCountdown.isEmpty()) {
            if (anyPieceOnTrack) {
                spawnCountdown = Optional.of(SPAWN_DELAY_ROUNDS); // A-28: timer starts now
            }
            return Optional.empty();
        }
        int remaining = spawnCountdown.get() - 1;
        if (remaining > 0) {
            spawnCountdown = Optional.of(remaining);
            return Optional.empty();
        }
        spawnCountdown = Optional.empty();
        spawn(emptyTrackCells, picker);
        return Optional.of(new MysteryCellTick(location.orElseThrow(), roundsRemaining, true));
    }

    // A-28: valid cells are empty at spawn time and are not the previous location.
    private void spawn(List<Integer> emptyTrackCells, RandomPicker picker) {
        List<Integer> candidates = emptyTrackCells.stream()
                .filter(cellIndex -> previousLocation.isEmpty() || !previousLocation.get().equals(cellIndex))
                .toList();
        location = Optional.of(picker.pick(candidates));
        roundsRemaining = SPAWN_DURATION_ROUNDS;
    }
}
