package ludot.app;

import ludot.output.ConsoleReporter;

import java.io.PrintStream;

/**
 * Entry point: no user interaction (CLAUDE.md). Parses the optional {@code --seed <n>}
 * argument, builds one game via {@link GameFactory}, and runs it to completion with output
 * going through {@link ConsoleReporter} on {@link System#out} — the only writer of game
 * output, per DESIGN.md §4.3.
 */
public final class Main {

    static final int USAGE_ERROR_EXIT_CODE = 2;
    private static final String SEED_FLAG = "--seed";

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Does the real work and returns an exit code instead of calling {@link System#exit}
     * itself, so tests can exercise the error path (CLAUDE.md's one documented exception for
     * {@code System.exit}) without forking a JVM. {@code main} is the only caller that actually
     * exits the process.
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        long seed;
        try {
            seed = resolveSeed(args);
        } catch (IllegalArgumentException e) {
            err.println("Usage: java -jar ludo-t.jar [--seed <n>]");
            return USAGE_ERROR_EXIT_CODE;
        }

        GameSession session = GameFactory.newGame(seed);
        session.events().subscribe(new ConsoleReporter(out));
        session.run();
        return 0;
    }

    /**
     * {@code --seed <n>} is optional. When absent, reads {@link System#nanoTime()} once to pick
     * a default — the one documented exception (CLAUDE.md) to "no Random/currentTimeMillis
     * outside ludot.random": this is seed selection in the composition root, not game logic.
     * Package-private so tests can exercise every parse outcome without going through
     * {@link #main}'s exit branch.
     */
    static long resolveSeed(String[] args) {
        if (args.length == 0) {
            return System.nanoTime();
        }
        if (args.length == 2 && SEED_FLAG.equals(args[0])) {
            try {
                return Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Seed must be a number, was: " + args[1], e);
            }
        }
        throw new IllegalArgumentException("Unrecognised arguments: " + String.join(" ", args));
    }
}
