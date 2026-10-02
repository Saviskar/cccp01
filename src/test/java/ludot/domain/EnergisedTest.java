package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergisedTest {

    @Test
    @DisplayName("t12_a32: Energised doubles the roll")
    void t12_a32_adjustStepsDoublesRoll() {
        Energised energised = new Energised();
        for (int roll = 1; roll <= 6; roll++) {
            assertEquals(roll * 2, energised.adjustSteps(roll));
        }
    }

    @Test
    @DisplayName("t12_a32: Energised never prevents movement")
    void t12_a32_canAlwaysMove() {
        assertTrue(new Energised().canMove());
    }

    @Test
    @DisplayName("t12_a32: the creation round's own round-end tick is absorbed without expiring")
    void t12_a32_firstRoundEndTickDoesNotExpire() {
        PieceEffect afterFirstTick = new Energised().onRoundEnd();

        assertEquals(new Energised(4), afterFirstTick);
    }

    @Test
    @DisplayName("t12_a32: the effect survives 4 full rounds after the teleport, expiring on the 5th tick")
    void t12_a32_expiresOnTheFifthRoundEndTick() {
        PieceEffect effect = new Energised();
        for (int tick = 1; tick <= 4; tick++) {
            effect = effect.onRoundEnd();
            assertTrue(effect instanceof Energised, "should still be active before the 5th tick");
        }

        effect = effect.onRoundEnd(); // 5th tick: round N+4's end

        assertEquals(new NoEffect(), effect);
    }
}
