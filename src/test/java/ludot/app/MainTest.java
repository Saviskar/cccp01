package ludot.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    @DisplayName("no arguments: two resolutions of the default seed differ")
    void noArgumentsResolvesADifferentDefaultSeedEachTime() {
        long first = Main.resolveSeed(new String[] {});
        long second = Main.resolveSeed(new String[] {});

        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("--seed <n>: the exact value is used")
    void seedFlagUsesTheGivenValue() {
        assertEquals(42L, Main.resolveSeed(new String[] {"--seed", "42"}));
    }

    @Test
    @DisplayName("--seed accepts a negative value")
    void seedFlagAcceptsANegativeValue() {
        assertEquals(-7L, Main.resolveSeed(new String[] {"--seed", "-7"}));
    }

    @Test
    @DisplayName("--seed with no value is rejected")
    void seedFlagWithNoValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Main.resolveSeed(new String[] {"--seed"}));
    }

    @Test
    @DisplayName("--seed with a non-numeric value is rejected")
    void seedFlagWithANonNumericValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Main.resolveSeed(new String[] {"--seed", "notanumber"}));
    }

    @Test
    @DisplayName("an unrecognised argument is rejected")
    void unrecognisedArgumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Main.resolveSeed(new String[] {"--bogus"}));
    }

    @Test
    @DisplayName("happy path: Main.run plays a full game, announces the seed first, and returns 0")
    void happyPathRunsAFullGameAndAnnouncesTheSeedFirst() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exitCode = Main.run(
                new String[] {"--seed", "7"},
                new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errBuffer, true, StandardCharsets.UTF_8));

        String output = outBuffer.toString(StandardCharsets.UTF_8);
        List<String> lines = output.lines().toList();
        assertEquals(0, exitCode);
        assertEquals("This run uses seed 7.", lines.get(0));
        assertEquals("Rerun with --seed 7 to reproduce this exact game.", lines.get(1));
        assertTrue(lines.size() > 2);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    @DisplayName("error path: Main.run reports a malformed --seed on stderr and returns the usage exit code")
    void errorPathReportsAMalformedSeedAndReturnsUsageExitCode() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exitCode = Main.run(
                new String[] {"--bogus"},
                new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errBuffer, true, StandardCharsets.UTF_8));

        assertEquals(Main.USAGE_ERROR_EXIT_CODE, exitCode);
        assertEquals("Usage: java -jar ludo-t.jar [--seed <n>]" + System.lineSeparator(),
                errBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).isEmpty());
    }
}
