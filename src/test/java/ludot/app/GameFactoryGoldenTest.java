package ludot.app;

import ludot.output.ConsoleReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * DESIGN.md §10's golden-file test for a fixed seed, now that phase 7 wires a real end-to-end
 * game: {@link GameFactory#newGame} for seed 9, run through a fresh {@link ConsoleReporter},
 * must match the approved transcript at {@code src/test/resources/golden/seed-9.txt} exactly.
 * Seed 9 was chosen (over seed 1, originally generated) as the shortest of seeds 1-20 whose
 * natural-finish game still exercises every rarer event family at least once: a block move, a
 * blockade break, a partial move, a capture, a mystery teleport, and Alpha/Beta/Gamma effects.
 */
class GameFactoryGoldenTest {

    private static final long GOLDEN_SEED = 9;
    private static final String GOLDEN_RESOURCE = "/golden/seed-9.txt";

    @Test
    @DisplayName("golden transcript: seed 9 matches the approved end-to-end output exactly")
    void seed9MatchesTheApprovedTranscript() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        GameSession session = GameFactory.newGame(GOLDEN_SEED);
        session.events().subscribe(new ConsoleReporter(new PrintStream(captured, true, StandardCharsets.UTF_8)));

        session.run();

        String actual = captured.toString(StandardCharsets.UTF_8);
        String expected = readGoldenResource();
        assertLinesEqual(expected, actual);
    }

    private static void assertLinesEqual(String expected, String actual) {
        List<String> expectedLines = expected.lines().toList();
        List<String> actualLines = actual.lines().toList();
        int firstMismatch = firstMismatch(expectedLines, actualLines);
        if (firstMismatch >= 0) {
            fail("Transcript differs from " + GOLDEN_RESOURCE + " at line " + (firstMismatch + 1) + ":"
                    + System.lineSeparator() + "expected: " + lineOrEof(expectedLines, firstMismatch)
                    + System.lineSeparator() + "actual:   " + lineOrEof(actualLines, firstMismatch));
        }
        assertEquals(expectedLines.size(), actualLines.size(), "transcript line count differs");
    }

    private static int firstMismatch(List<String> expected, List<String> actual) {
        int limit = Math.min(expected.size(), actual.size());
        for (int i = 0; i < limit; i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                return i;
            }
        }
        return expected.size() == actual.size() ? -1 : limit;
    }

    private static String lineOrEof(List<String> lines, int index) {
        return index < lines.size() ? lines.get(index) : "<end of file>";
    }

    private static String readGoldenResource() {
        try (InputStream in = GameFactoryGoldenTest.class.getResourceAsStream(GOLDEN_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + GOLDEN_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
