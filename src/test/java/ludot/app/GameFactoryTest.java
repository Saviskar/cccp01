package ludot.app;

import ludot.events.GameEnded;
import ludot.events.GameEventListener;
import ludot.output.ConsoleReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Phase 7's "end-to-end runs" requirement: {@link GameFactory} wires a complete, real game
 * (real seeded Dice/Coin/RandomPicker, real strategies) that plays to completion and whose
 * output is fully determined by the seed.
 */
class GameFactoryTest {

    private String runToTranscript(long seed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GameSession session = GameFactory.newGame(seed);
        session.events().subscribe(new ConsoleReporter(new PrintStream(out, true, StandardCharsets.UTF_8)));
        session.run();
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("CLAUDE.md: the same seed produces identical output")
    void sameSeedProducesIdenticalOutput() {
        String first = runToTranscript(1);
        String second = runToTranscript(1);

        assertEquals(first, second);
        assertFalse(first.isBlank());
    }

    @Test
    @DisplayName("a wired game runs to completion, publishing exactly one GameEnded")
    void aWiredGameRunsToCompletion() {
        for (long seed : new long[] {1, 2, 3}) {
            GameSession session = GameFactory.newGame(seed);
            AtomicInteger gameEndedCount = new AtomicInteger();
            GameEventListener counter = event -> {
                if (event instanceof GameEnded) {
                    gameEndedCount.incrementAndGet();
                }
            };
            session.events().subscribe(counter);

            session.run();

            assertEquals(1, gameEndedCount.get(), "seed " + seed + " should publish exactly one GameEnded");
        }
    }
}
