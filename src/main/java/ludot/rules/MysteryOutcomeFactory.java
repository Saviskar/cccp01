package ludot.rules;

import ludot.random.RandomPicker;

import java.util.List;

/** T-11/A-30: picks one of the six teleport outcomes, each with equal probability. */
public final class MysteryOutcomeFactory {

    private final List<MysteryOutcome> outcomes = List.of(
            new TeleportToAlpha(), new TeleportToBeta(), new TeleportToGamma(), new TeleportToBase(),
            new TeleportToX(), new TeleportToApproach());

    public MysteryOutcome choose(RandomPicker picker) {
        return picker.pick(outcomes);
    }
}
