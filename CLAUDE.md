# CLAUDE.md — LUDO-T Simulation

## Project
A command-line simulation of LUDO-T (Ludo with extended rules). It runs with no user interaction: four AI players (Red, Green, Yellow, Blue) play until completion, and the program prints the event log specified in the brief.

## Tech stack
- **Java 21** (LTS). Set `<maven.compiler.release>21</maven.compiler.release>` in `pom.xml`. Do not use any language feature or API added after Java 21.
- **No preview or incubator features** (no `--enable-preview`). Only features finalised in Java 21, so the code compiles and runs with a plain `mvn package` on the marker's machine. In particular: no unnamed variables/patterns (`_`), no string templates, no unnamed classes or instance `main` methods — these are preview-only in 21.
- **Maven** build. Tests use **JUnit 5** and **Mockito 5** (`mockito-core` + `mockito-junit-jupiter`). No other third-party dependencies.
- On Java 21, configure `maven-surefire-plugin` to load Mockito as a Java agent (`-javaagent` in `argLine`), so tests run without the dynamic-agent-loading warning.
- Root package: `ludot`, with subpackages matching `DESIGN.md` 2.2 (`ludot.domain`, `ludot.board`, `ludot.moves`, `ludot.rules`, `ludot.players`, `ludot.engine`, `ludot.events`, `ludot.output`, `ludot.random`, `ludot.app`).
- Entry point: `ludot.app.Main`. Build a runnable jar: `mvn package`, run with `java -jar target/ludo-t.jar [--seed <n>]`.
- Run tests with `mvn test`.

## Java conventions
- **Records** for immutable value types: `PieceId`, `Move` data, all `GameEvent` types.
- **Sealed interfaces** for closed variant sets: `Position` (`InBase`, `OnTrack`, `InHomeStraight`, `AtHome`), `GameEvent`.
- **Pattern matching `switch`** with record patterns over sealed types (e.g. formatting each `GameEvent` in `ConsoleReporter`, handling each `Position` in `MovementCalculator`). Rely on exhaustiveness checking — no `default` branch on sealed switches, so adding a variant is a compile error until it is handled.
- **Enums** for `Colour` and `Direction`; colour offsets computed from the enum (A-02).
- Named constants for every rule number (`TRACK_SIZE = 52`, `HOME_STRAIGHT_LENGTH = 5`, `MAX_ROUNDS = 1000`, etc.). No magic numbers.
- `final` fields by default; no public setters; constructor injection only.
- Only the seeded implementations in `ludot.random` may use `java.util.Random`. Nothing else may use `Random`, `Math.random()` or `System.currentTimeMillis()` for game logic.
- Only `ConsoleReporter` may use `System.out`.
- No `null` for "no effect" — use `NoEffect`. Use `Optional` for genuinely optional values (e.g. mystery cell location).
- Keep methods under ~25 lines; extract well-named private methods instead.

## Sources of truth (in priority order)
1. `docs/SPEC.md` — the assignment brief, converted to markdown.
2. `ASSUMPTIONS.md` — **binding** interpretations of every ambiguity, with IDs `A-01` … `A-45`.
3. `DESIGN.md` — **binding** architecture: packages, class responsibilities, data structures, patterns, control flow. The summary below is a quick reference; `DESIGN.md` wins if they differ.
4. This file — working rules.

**Never invent a rule interpretation.** If something is not covered by the spec or `ASSUMPTIONS.md`, stop and ask me. Do not guess and do not silently pick an option. When code implements an assumption, add a comment referencing its ID (e.g. `// A-17`).

## Architecture (fixed — do not deviate without asking)

### Core rule: the engine decides what is legal, strategies decide what is preferred
- `MoveGenerator` produces **every legal move** for the current roll, applying all rules and assumptions.
- `PlayerStrategy` receives that list and **only ranks/selects** a move from it. A strategy must never check or enforce game rules, and must never mutate game state.
- The `GameEngine` applies the selected move and emits events.

### Components
| Component | Responsibility |
|---|---|
| `BoardTopology` | Pure geometry: track indices, X/Approach per colour, Alpha/Beta/Gamma, next-cell stepping in either direction. No game state. |
| `Board` / `GameState` | Cell occupancy, pieces, round counter, mystery cell state. Blocks are **derived** from occupancy, never stored (A-14). |
| `Piece` | Colour, number, location, original direction, capture count, ccw crossing counter, active effect. |
| `Move` (Command) | Concrete move types: `EnterFromBase`, `StepMove`, `PartialMove`, `BlockMove`, `BlockBreakMove`. Each knows its origin, destination and whether it captures / lands on the mystery cell. |
| `MoveGenerator` | Builds the legal move list for a player and roll. |
| `PlayerStrategy` (Strategy) | `RedStrategy`, `GreenStrategy`, `YellowStrategy`, `BlueStrategy`. |
| `PieceEffect` | Timed effects (`Energised`, `Sick`, `Briefing`) that count down on round end. |
| `MysteryOutcome` + factory | One class per teleport outcome (Alpha, Beta, Gamma, Base, X, Approach); a factory picks one via the injected RNG. |
| `GameEventListener` (Observer) | The engine emits typed events; `ConsoleReporter` turns them into the exact required strings. |
| `Dice`, `Coin` interfaces | Injected. Real implementations take a seed; tests stub them with Mockito. |

### Design rules
- **No output from game logic.** Only `ConsoleReporter` prints. All message templates live in one place.
- **No hidden randomness.** Every random decision (dice, coin, mystery spawn cell, teleport outcome, Alpha outcome, Blue's random choice) goes through injected interfaces.
- **No Singletons, no global state.**
- Do not add design patterns beyond those listed without asking. Each pattern must solve a concrete problem in this spec.
- Small, focused classes and interfaces (SRP, ISP). Depend on abstractions (DIP). A new player type must be addable without modifying the engine (OCP).
- Prefer clear names over comments; comments explain *why* or cite an assumption ID.

## Output
- Messages must match Section 3 of the brief **verbatim** (A-43), including its wording quirks.
- Messages not defined in the brief follow A-44.
- The executable takes an optional `--seed <n>` argument; the same seed must produce identical output.

## Workflow

### Rules for every phase
1. **Work on one phase only**, named by its ID below (e.g. "phase 4c"). Do not start the next phase.
2. **Plan mode first.** Present a plan and wait for my approval before writing code.
3. **Tests first.** Write the JUnit tests for the phase (following the Mockito rules under "Testing"), run them and confirm they fail, *then* implement until they pass.
4. **Never weaken a test to make it pass.** If a test looks wrong, stop and ask me.
5. **`docs/SPEC.md`, `ASSUMPTIONS.md` and `DESIGN.md` are read-only.** Never edit them unless I explicitly ask. If the code seems to need a change to them, stop and ask.
6. Keep changes small and focused. Do not refactor unrelated code.
7. Finish by running `mvn test` and reporting the full result. I will commit after the phase passes review.

### Phases

| Phase | Scope |
|---|---|
| 1 | Maven project setup; `Colour`, `Direction`, `BoardTopology` + tests for every index in A-02 and A-03 |
| 2 | `PieceId`, `Position`, `Piece`, `BoardState`, `Dice` / `Coin` / `RandomPicker` (seeded implementations), event types and `EventBus` |
| 3 | Traditional rules 1–11 via `MovementCalculator`, `MoveGenerator`, `LandingResolver`, `TurnController`, `RoundManager`, `GameEngine`. Use a temporary `FirstLegalMoveStrategy` (test-only) until phase 6 |
| 4a | T-1 — direction by coin toss, counterclockwise movement, second-crossing entry (A-08, A-12) |
| 4b | T-2 and T-9 — capture bonus roll, reset on return to base (A-23, A-24, A-26) |
| 4c | T-3 — blocks, obstruction, partial moves (A-14 to A-16) |
| 4d | T-4 and T-5 — block moves, breaking blocks (A-17 to A-19, A-21) |
| 4e | T-6 — three sixes with a block (A-22) |
| 4f | T-7 — capture required for home straight (A-07) |
| 4g | T-8 — block captures block (A-20) |
| 4h | T-10 and T-11 — mystery cell timing and teleport outcomes (A-28 to A-31) |
| 4i | T-12 — Alpha effects (A-32, A-45) |
| 4j | T-13 — Beta briefing (A-33) |
| 4k | T-14 and T-15 — Gamma (A-34, A-35) |
| 5 | `ConsoleReporter` + `MessageTemplates` + golden-file tests against `docs/SPEC.md` Section 3 |
| 6a | `RedStrategy` (A-36) |
| 6b | `GreenStrategy` (A-37) |
| 6c | `YellowStrategy` (A-38) |
| 6d | `BlueStrategy` (A-39) |
| 7 | `Main`, `GameFactory`, `--seed`, round guard (A-41, A-42), end-to-end runs, 1,000-game invariant stress test |

### Testing
**Mockito rules**
- **Mock roles, not values.** Mock only interfaces at the boundaries of the unit under test: `Dice`, `Coin`, `RandomPicker`, `PlayerStrategy`, `GameEventListener`, and `GameView` (for strategy tests).
- **Never mock** records, enums, `Piece`, `BoardState`, `BoardTopology` or `MysteryCell`. Build real instances with small test helpers. Mocking them would hide real rule bugs.
- Use `@ExtendWith(MockitoExtension.class)` with the default strict stubs, so unused stubs fail the test.
- Stub random sequences with consecutive returns, e.g. `when(dice.roll()).thenReturn(6, 6, 6)` for T-6, `when(coin.toss()).thenReturn(Direction.COUNTERCLOCKWISE)` for T-1.
- Verify output-relevant behaviour through the event listener: `ArgumentCaptor<GameEvent>` for event contents, `InOrder` for event order.
- Use `verify(...)` only for interactions that matter to the rule (e.g. the strategy is called with exactly the legal moves; a Beta piece is never offered as movable). Do not verify incidental calls.
- `FirstLegalMoveStrategy` stays a small hand-written fake (not a mock), because phases 3–5 need real multi-turn games before the real strategies exist. The phase 7 stress test uses the four real strategies.

**General**
- Every rule and every assumption gets at least one test. Name JUnit tests after the rule/assumption ID (e.g. `A17_blockMoveUsesFloorDivision()` with `@DisplayName("A-17: block move uses floor(roll / n)")`).
- The phase 7 stress test runs 1,000 seeded games and asserts: each colour always has exactly 4 pieces across base/track/home straight/home, no two opposing pieces share a track cell, no crashes, and every game ends within the round guard.

## Definition of done (per phase)
- All tests pass.
- No rule logic inside strategies; no printing outside the reporter.
- Any new assumption was approved by me and added to `ASSUMPTIONS.md` first.
