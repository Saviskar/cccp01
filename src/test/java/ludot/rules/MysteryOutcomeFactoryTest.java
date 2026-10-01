package ludot.rules;

import ludot.domain.MysteryOutcomeKind;
import ludot.random.RandomPicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MysteryOutcomeFactoryTest {

    @Mock
    private RandomPicker picker;

    private final MysteryOutcomeFactory factory = new MysteryOutcomeFactory();

    @Test
    @DisplayName("T-11: offers all six outcomes, each exactly once, for A-30's equal-probability pick")
    void t11_offersAllSixOutcomesExactlyOnce() {
        when(picker.pick(anyList())).thenAnswer(inv -> {
            List<MysteryOutcome> options = inv.getArgument(0);
            return options.get(0);
        });

        factory.choose(picker);

        ArgumentCaptor<List<MysteryOutcome>> captor = ArgumentCaptor.forClass(List.class);
        verify(picker).pick(captor.capture());
        Set<MysteryOutcomeKind> kinds =
                captor.getValue().stream().map(MysteryOutcome::kind).collect(Collectors.toSet());
        assertEquals(Set.of(MysteryOutcomeKind.values()), kinds);
        assertEquals(6, captor.getValue().size());
    }

    @Test
    @DisplayName("A-30: choose returns exactly what the picker picks")
    void a30_chooseReturnsPickerChoice() {
        MysteryOutcome chosen = new TeleportToBase();
        when(picker.pick(anyList())).thenAnswer(inv -> chosen);

        MysteryOutcome result = factory.choose(picker);

        assertEquals(chosen, result);
    }
}
