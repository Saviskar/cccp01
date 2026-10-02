package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SickTest {

    @Test
    @DisplayName("t12_a32: Sick halves the roll, floored")
    void t12_a32_adjustStepsHalvesRollFloored() {
        Sick sick = new Sick();
        for (int roll = 1; roll <= 6; roll++) {
            assertEquals(roll / 2, sick.adjustSteps(roll));
        }
    }

    @Test
    @DisplayName("t12_a32: Sick never prevents movement outright (an effective 0 is a MoveGenerator concern)")
    void t12_a32_canAlwaysMove() {
        assertTrue(new Sick().canMove());
    }

    @Test
    @DisplayName("t12_a32: the creation round's own round-end tick is absorbed without expiring")
    void t12_a32_firstRoundEndTickDoesNotExpire() {
        PieceEffect afterFirstTick = new Sick().onRoundEnd();

        assertEquals(new Sick(4), afterFirstTick);
    }

    @Test
    @DisplayName("t12_a32: the effect survives 4 full rounds after the teleport, expiring on the 5th tick")
    void t12_a32_expiresOnTheFifthRoundEndTick() {
        PieceEffect effect = new Sick();
        for (int tick = 1; tick <= 4; tick++) {
            effect = effect.onRoundEnd();
            assertTrue(effect instanceof Sick, "should still be active before the 5th tick");
        }

        effect = effect.onRoundEnd(); // 5th tick: round N+4's end

        assertEquals(new NoEffect(), effect);
    }
}
