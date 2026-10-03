package ludot.app;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Colour;
import ludot.engine.GameEngine;
import ludot.engine.RoundManager;
import ludot.engine.TurnController;
import ludot.events.EventBus;
import ludot.players.BlueStrategy;
import ludot.players.GreenStrategy;
import ludot.players.MoveRanking;
import ludot.players.PlayerStrategy;
import ludot.players.RedStrategy;
import ludot.players.YellowStrategy;
import ludot.random.RandomPicker;
import ludot.random.SeededCoin;
import ludot.random.SeededDice;
import ludot.random.SeededPicker;
import ludot.rules.BlockBreakPlanner;
import ludot.rules.LandingResolver;
import ludot.rules.MoveGenerator;
import ludot.rules.MovementCalculator;
import ludot.rules.MysteryOutcomeFactory;
import ludot.rules.MysteryResolver;

import java.util.EnumMap;
import java.util.Map;

/**
 * Composition root (DESIGN.md §2.3/§4.6): the only class that wires every concrete collaborator
 * together from a seed. Nothing in {@code ludot.app} otherwise names a concrete rule/random class.
 */
public final class GameFactory {

    // Distinct derived seeds per random source, so Dice/Coin/RandomPicker are independent
    // Random streams rather than three instances replaying identical internal state.
    private static final long COIN_SEED_OFFSET = 1;
    private static final long PICKER_SEED_OFFSET = 2;

    private GameFactory() {
    }

    public static GameSession newGame(long seed) {
        SeededDice dice = new SeededDice(seed);
        SeededCoin coin = new SeededCoin(seed + COIN_SEED_OFFSET);
        RandomPicker picker = new SeededPicker(seed + PICKER_SEED_OFFSET);

        BoardTopology topology = new BoardTopology();
        LandingResolver landingResolver = new LandingResolver();
        MoveGenerator moveGenerator = new MoveGenerator(new MovementCalculator());
        MysteryResolver mysteryResolver =
                new MysteryResolver(new MysteryOutcomeFactory(), picker, topology, landingResolver);

        EventBus events = new EventBus();
        TurnController turnController = new TurnController(
                dice, coin, moveGenerator, topology, events, landingResolver, new BlockBreakPlanner(),
                mysteryResolver);
        RoundManager roundManager = new RoundManager(turnController, events, picker);

        MoveRanking ranking = new MoveRanking();
        Map<Colour, PlayerStrategy> strategies = new EnumMap<>(Colour.class);
        strategies.put(Colour.RED, new RedStrategy(ranking));
        strategies.put(Colour.GREEN, new GreenStrategy(ranking));
        strategies.put(Colour.YELLOW, new YellowStrategy(ranking));
        strategies.put(Colour.BLUE, new BlueStrategy(picker));

        GameEngine engine = new GameEngine(seed, dice, roundManager, events, strategies);
        return new GameSession(engine, new BoardState(), events);
    }
}
