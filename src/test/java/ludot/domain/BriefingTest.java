package ludot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BriefingTest {

    @Test
    @DisplayName("t13_a33: Briefing never allows movement")
    void t13_a33_canNeverMove() {
        assertFalse(new Briefing().canMove());
    }

    @Test
    @DisplayName("t13_a33: adjustSteps is identity (unreachable in practice, since canMove() is false)")
    void t13_a33_adjustStepsIsIdentity() {
        Briefing briefing = new Briefing();
        for (int roll = 1; roll <= 6; roll++) {
            assertEquals(roll, briefing.adjustSteps(roll));
        }
    }

    @Test
    @DisplayName("t13_a33: the creation round's own round-end tick is absorbed without expiring")
    void t13_a33_firstRoundEndTickDoesNotExpire() {
        PieceEffect afterFirstTick = new Briefing().onRoundEnd();

        assertEquals(new Briefing(4), afterFirstTick);
    }

    @Test
    @DisplayName("t13_a33: the effect survives 4 full rounds after the teleport, expiring on the 5th tick")
    void t13_a33_expiresOnTheFifthRoundEndTick() {
        PieceEffect effect = new Briefing();
        for (int tick = 1; tick <= 4; tick++) {
            effect = effect.onRoundEnd();
            assertTrue(effect instanceof Briefing, "should still be active before the 5th tick");
        }

        effect = effect.onRoundEnd(); // 5th tick: round N+4's end

        assertEquals(new NoEffect(), effect);
    }
}
