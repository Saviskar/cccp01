package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Briefing;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.BriefingAssigned;
import ludot.events.EventBus;
import ludot.events.GameEvent;
import ludot.events.GameEventListener;
import ludot.events.GammaDirectionReversed;
import ludot.events.GammaRerouteTriggered;
import ludot.events.PieceCaptured;
import ludot.events.PieceTeleported;
import ludot.random.FirstItemPicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeleportToGammaTest {

    @Mock
    private GameEventListener listener;

    private BoardState board;
    private MysteryContext context;
    private final TeleportToGamma outcome = new TeleportToGamma(new TeleportToBeta());
    private final BoardTopology topology = new BoardTopology();

    @BeforeEach
    void setUp() {
        board = new BoardState();
        EventBus events = new EventBus();
        events.subscribe(listener);
        context = new MysteryContext(board, topology, events, new LandingResolver(), new FirstItemPicker());
    }

    @Test
    @DisplayName("kind() is GAMMA")
    void kindIsGamma() {
        assertEquals(MysteryOutcomeKind.GAMMA, outcome.kind());
    }

    @Test
    @DisplayName("T-11: teleports the piece onto the Gamma cell")
    void teleportsPieceOntoGammaCell() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.CLOCKWISE);

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.gammaIndex()), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: an opponent block on Gamma sends the teleported piece to base")
    void opponentBlockOnGammaSendsPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.gammaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.gammaIndex()));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("A-31: a single opponent piece on Gamma is captured")
    void singleOpponentOnGammaIsCaptured() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.CLOCKWISE);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.gammaIndex()));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
    }

    @Test
    @DisplayName("t14_a34: a clockwise piece becomes counterclockwise")
    void t14_a34_clockwisePieceBecomesCounterclockwise() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.CLOCKWISE);

        outcome.apply(id, context);

        assertEquals(Direction.COUNTERCLOCKWISE, board.piece(id).originalDirection().orElseThrow());
    }

    @Test
    @DisplayName("t14_a34: the clockwise branch publishes GammaDirectionReversed and no Beta events")
    void t14_a34_clockwiseBranchPublishesGammaDirectionReversed() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.CLOCKWISE);

        outcome.apply(id, context);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(GammaDirectionReversed.class::isInstance));
        assertFalse(captor.getAllValues().stream().anyMatch(GammaRerouteTriggered.class::isInstance));
        assertFalse(captor.getAllValues().stream().anyMatch(BriefingAssigned.class::isInstance));
        long teleportCount = captor.getAllValues().stream().filter(PieceTeleported.class::isInstance).count();
        assertEquals(1, teleportCount);
    }

    @Test
    @DisplayName("t14_a34: a counterclockwise piece is rerouted on to Beta")
    void t14_a34_counterclockwisePieceIsReroutedToBeta() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);

        outcome.apply(id, context);

        assertEquals(new OnTrack(topology.betaIndex()), board.piece(id).position());
    }

    @Test
    @DisplayName("t14_a34: the reroute to Beta applies the Briefing effect")
    void t14_a34_rerouteAppliesBriefingEffect() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);

        outcome.apply(id, context);

        assertEquals(new Briefing(), board.piece(id).effect());
    }

    @Test
    @DisplayName("t14_a34: the reroute publishes teleport-to-Gamma, reroute, teleport-to-Beta, then briefing in order")
    void t14_a34_rerouteEventOrder() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);

        outcome.apply(id, context);

        InOrder order = inOrder(listener);
        order.verify(listener).onEvent(any(PieceTeleported.class));
        order.verify(listener).onEvent(any(GammaRerouteTriggered.class));
        order.verify(listener).onEvent(any(PieceTeleported.class));
        order.verify(listener).onEvent(any(BriefingAssigned.class));
    }

    @Test
    @DisplayName("t14_a31: a reroute blocked by an opponent block on Beta sends the piece to base instead")
    void t14_a31_rerouteBlockedByOpponentBlockOnBetaGoesToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.betaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.betaIndex()));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertFalse(captor.getAllValues().stream().anyMatch(BriefingAssigned.class::isInstance));
    }

    @Test
    @DisplayName("t14_a58: capturing the opponent on Gamma still reroutes the piece on to Beta")
    void t14_a58_captureOnGammaThenRerouteToBeta() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.gammaIndex()));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(opponent).position());
        assertEquals(new OnTrack(topology.betaIndex()), board.piece(id).position());
        assertEquals(new Briefing(), board.piece(id).effect());
    }

    @Test
    @DisplayName("t14_a58: an opponent block on Gamma ends the chain before Beta")
    void t14_a58_opponentBlockOnGammaEndsChain() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(topology.gammaIndex()));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(topology.gammaIndex()));

        outcome.apply(id, context);

        assertEquals(new InBase(), board.piece(id).position());
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        assertFalse(captor.getAllValues().stream().anyMatch(GammaRerouteTriggered.class::isInstance));
        assertFalse(captor.getAllValues().stream().anyMatch(BriefingAssigned.class::isInstance));
        long teleportCount = captor.getAllValues().stream().filter(PieceTeleported.class::isInstance).count();
        assertEquals(1, teleportCount);
    }

    @Test
    @DisplayName("t14_a58: capturing opponents at both Gamma and Beta counts both captures but signals one")
    void t14_a58_captureAtBothGammaAndBetaCountsBothCapturesButOneSignal() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.COUNTERCLOCKWISE);
        PieceId gammaOpponent = new PieceId(Colour.GREEN, 1);
        PieceId betaOpponent = new PieceId(Colour.GREEN, 2);
        board.moveTo(gammaOpponent, new OnTrack(topology.gammaIndex()));
        board.moveTo(betaOpponent, new OnTrack(topology.betaIndex()));

        boolean captured = outcome.apply(id, context);

        assertTrue(captured);
        assertEquals(new InBase(), board.piece(gammaOpponent).position());
        assertEquals(new InBase(), board.piece(betaOpponent).position());
        assertEquals(new OnTrack(topology.betaIndex()), board.piece(id).position());
        assertEquals(new Briefing(), board.piece(id).effect());
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(listener, atLeastOnce()).onEvent(captor.capture());
        long captureCount = captor.getAllValues().stream().filter(PieceCaptured.class::isInstance).count();
        assertEquals(2, captureCount);
    }

    @Test
    @DisplayName("t14_a58: a piece switched to counterclockwise at Gamma needs two counterclockwise Approach "
            + "crossings before entering the home straight")
    void t14_a58_switchedPieceNeedsTwoCounterclockwiseCrossings() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.assignDirection(id, Direction.CLOCKWISE);
        board.recordCapture(id); // A-07: satisfied so only A-08 is under test

        outcome.apply(id, context); // switches to COUNTERCLOCKWISE at Gamma

        assertEquals(0, board.piece(id).ccwApproachCrossings());

        int steps = Math.floorMod(
                topology.gammaIndex() - topology.approachIndex(Colour.RED), BoardTopology.TRACK_SIZE) + 1;
        MovementCalculator calculator = new MovementCalculator();
        RouteResult result = calculator.walk(
                board.piece(id).position(), steps, Colour.RED, Direction.COUNTERCLOCKWISE,
                board.piece(id).ccwApproachCrossings(), board.piece(id).captureCount(), topology, board);

        RouteResult.Reachable reachable = assertInstanceOf(RouteResult.Reachable.class, result);
        assertTrue(reachable.crossedApproachWithoutEntering());
        assertInstanceOf(OnTrack.class, reachable.destination());
    }
}
