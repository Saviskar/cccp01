package ludot.board;

import ludot.random.RandomPicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MysteryCellTest {

    @Mock
    private RandomPicker picker;

    private final MysteryCell cell = new MysteryCell();

    @Test
    @DisplayName("T-10: no spawn countdown starts while no piece has reached the standard track")
    void t10_noSpawnUntilAnyPieceReachesTrack() {
        Optional<MysteryCellTick> first = cell.onRoundEnd(false, List.of(0, 1, 2), picker);
        Optional<MysteryCellTick> second = cell.onRoundEnd(false, List.of(0, 1, 2), picker);

        assertEquals(Optional.empty(), first);
        assertEquals(Optional.empty(), second);
        verify(picker, never()).pick(anyList());
    }

    @Test
    @DisplayName("A-28: the timer starts at the round-end a piece first reaches the track, and spawns 2 round-ends later")
    void a28_timerStartsThenSpawnsTwoRoundsLater() {
        when(picker.pick(List.of(5, 6))).thenReturn(5);

        Optional<MysteryCellTick> trigger = cell.onRoundEnd(true, List.of(5, 6), picker); // starts the timer
        Optional<MysteryCellTick> oneMore = cell.onRoundEnd(false, List.of(5, 6), picker); // 1 round-end to go
        Optional<MysteryCellTick> spawns = cell.onRoundEnd(false, List.of(5, 6), picker); // spawns now

        assertEquals(Optional.empty(), trigger);
        assertEquals(Optional.empty(), oneMore);
        assertEquals(Optional.of(new MysteryCellTick(5, 4, true)), spawns);
    }

    @Test
    @DisplayName("A-28: the first spawn picks only from the given empty cells, with no previous location to exclude")
    void a28_firstSpawnPicksFromEmptyCellsOnly() {
        List<Integer> emptyCells = List.of(10, 20, 30);
        when(picker.pick(emptyCells)).thenReturn(20);

        cell.onRoundEnd(true, emptyCells, picker);
        cell.onRoundEnd(true, emptyCells, picker);
        Optional<MysteryCellTick> spawns = cell.onRoundEnd(true, emptyCells, picker);

        assertEquals(Optional.of(new MysteryCellTick(20, 4, true)), spawns);
    }

    @Test
    @DisplayName("A-28: stays active for 4 rounds (4, 3, 2, 1), never reporting 0")
    void a28_staysFourRoundsThenRespawnsImmediately() {
        when(picker.pick(List.of(1, 2))).thenReturn(1); // first spawn picks 1
        when(picker.pick(List.of(2))).thenReturn(2); // respawn excludes 1, so only 2 remains
        cell.onRoundEnd(true, List.of(1, 2), picker);
        cell.onRoundEnd(true, List.of(1, 2), picker);
        cell.onRoundEnd(true, List.of(1, 2), picker); // spawns at location 1, 4 rounds remaining

        Optional<MysteryCellTick> three = cell.onRoundEnd(false, List.of(2), picker);
        Optional<MysteryCellTick> two = cell.onRoundEnd(false, List.of(2), picker);
        Optional<MysteryCellTick> one = cell.onRoundEnd(false, List.of(2), picker);
        Optional<MysteryCellTick> respawned = cell.onRoundEnd(false, List.of(2), picker);

        assertEquals(Optional.of(new MysteryCellTick(1, 3, false)), three);
        assertEquals(Optional.of(new MysteryCellTick(1, 2, false)), two);
        assertEquals(Optional.of(new MysteryCellTick(1, 1, false)), one);
        assertEquals(Optional.of(new MysteryCellTick(2, 4, true)), respawned);
    }

    @Test
    @DisplayName("A-28: a respawn excludes the cell it just vacated from the candidate list")
    void a28_respawnExcludesPreviousLocation() {
        when(picker.pick(List.of(1, 2))).thenReturn(1);
        cell.onRoundEnd(true, List.of(1, 2), picker);
        cell.onRoundEnd(true, List.of(1, 2), picker);
        cell.onRoundEnd(true, List.of(1, 2), picker); // spawns at 1

        when(picker.pick(List.of(2))).thenReturn(2);
        cell.onRoundEnd(false, List.of(1, 2), picker);
        cell.onRoundEnd(false, List.of(1, 2), picker);
        cell.onRoundEnd(false, List.of(1, 2), picker);
        Optional<MysteryCellTick> respawned = cell.onRoundEnd(false, List.of(1, 2), picker); // must exclude 1

        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(picker, times(2)).pick(captor.capture());
        assertEquals(List.of(2), captor.getAllValues().get(1));
        assertTrue(respawned.isPresent());
        assertEquals(2, respawned.get().location());
    }

    @Test
    @DisplayName("location() reflects the currently active cell, empty before the first spawn")
    void locationReflectsCurrentCell() {
        assertEquals(Optional.empty(), cell.location());
        when(picker.pick(List.of(7))).thenReturn(7);
        cell.onRoundEnd(true, List.of(7), picker);
        cell.onRoundEnd(true, List.of(7), picker);
        cell.onRoundEnd(true, List.of(7), picker);
        assertEquals(Optional.of(7), cell.location());
    }
}
