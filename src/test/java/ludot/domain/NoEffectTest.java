package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoEffectTest {

    private final NoEffect noEffect = new NoEffect();

    @Test
    @DisplayName("NoEffect leaves the roll unchanged")
    void adjustStepsIsIdentity() {
        for (int roll = 1; roll <= 6; roll++) {
            assertEquals(roll, noEffect.adjustSteps(roll));
        }
    }

    @Test
    @DisplayName("NoEffect never prevents movement")
    void canAlwaysMove() {
        assertTrue(noEffect.canMove());
    }

    @Test
    @DisplayName("NoEffect stays NoEffect at round end")
    void onRoundEndStaysNoEffect() {
        assertEquals(new NoEffect(), noEffect.onRoundEnd());
    }
}
