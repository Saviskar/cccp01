package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.board.GameView;
import ludot.domain.AtHome;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InHomeStraight;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.NoLegalMove;
import ludot.events.PlayerFinished;
import ludot.events.ThirdSixIgnored;
import ludot.players.FirstLegalMoveStrategy;
import ludot.random.Dice;
import ludot.rules.LandingResolver;
import ludot.rules.MoveGenerator;
import ludot.rules.MovementCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnControllerTest {

    @Mock
    private Dice dice;

    @Mock
    private GameEventListener listener;

    private BoardTopology topology;
    private BoardState board;
    private EventBus events;
    private Standings standings;
    private TurnController controller;
    private Player player;

    @BeforeEach
    void setUp() {
        topology = new BoardTopology();
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
        standings = new Standings();
        MoveGenerator moveGenerator = new MoveGenerator(new MovementCalculator());
        controller = new TurnController(dice, moveGenerator, topology, events, new LandingResolver());
        player = new Player(Colour.RED, new FirstLegalMoveStrategy());
    }

    private GameView view() {
        return board;
    }

    @Test
    @DisplayName("a normal roll with a legal move ends the turn after one roll")
    void normalRollEndsAfterOneRoll() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(4);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll();
        assertEquals(new OnTrack(14), board.piece(id).position());
    }

    @Test
    @DisplayName("Rule 3: with no six rolled across several turns, no piece leaves base onto a standard cell")
    void rule3_noPieceOnStandardCellsWithoutASix() {
        when(dice.roll()).thenReturn(1, 2, 3, 4, 5);

        for (int i = 0; i < 5; i++) {
            controller.playTurn(player, board, standings, view());
        }

        assertEquals(0, board.countOnBoard(Colour.RED));
    }

    @Test
    @DisplayName("Rule 4: a six grants a bonus roll")
    void rule4_sixGrantsBonusRoll() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 4);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(2)).roll();
    }

    @Test
    @DisplayName("A-47: a six with no legal move still grants a bonus roll")
    void a47_sixWithNoLegalMoveStillGrantsBonusRoll() {
        // Every piece is 1-4 steps from Home; a roll of 6 overshoots for all of them,
        // and none are in base, so a six produces zero legal moves.
        for (int number = 1; number <= 4; number++) {
            PieceId id = new PieceId(Colour.RED, number);
            board.moveTo(id, new InHomeStraight(number - 1));
            board.assignDirection(id, Direction.CLOCKWISE);
        }
        when(dice.roll()).thenReturn(6, 3);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(2)).roll();
        ArgumentCaptor<NoLegalMove> captor = ArgumentCaptor.forClass(NoLegalMove.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(Colour.RED, captor.getValue().colour());
        assertEquals(6, captor.getValue().rollValue());
    }

    @Test
    @DisplayName("Rule 4: a third consecutive six is ignored and the turn ends")
    void rule4_thirdConsecutiveSixIsIgnored() {
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll();
        // The third six is detected and ignored before any move is generated for it (Rule 4):
        // no NoLegalMove or move-related event is published for that third roll, only this one.
        verify(listener).onEvent(any(ThirdSixIgnored.class));
        assertEquals(1, board.countOnBoard(Colour.RED)); // R1 left base on the first six and stepped on the second
    }

    @Test
    @DisplayName("a roll with no legal move at all publishes NoLegalMove and ends the turn")
    void noLegalMovePublishesEventAndEndsTurn() {
        when(dice.roll()).thenReturn(3); // everything still in base, not a six

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll();
        ArgumentCaptor<NoLegalMove> captor = ArgumentCaptor.forClass(NoLegalMove.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(3, captor.getValue().rollValue());
    }

    @Test
    @DisplayName("Rule 11: reaching Home ends the turn immediately, even on a six")
    void rule11_reachingHomeEndsTurnImmediatelyEvenOnASix() {
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 3), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 4), new AtHome());
        PieceId last = new PieceId(Colour.RED, 1);
        board.moveTo(last, new OnTrack(topology.approachIndex(Colour.RED)));
        board.assignDirection(last, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6); // exactly 6 steps from Approach reaches Home

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll(); // no bonus roll despite the six: the turn already ended
        assertEquals(new AtHome(), board.piece(last).position());
        ArgumentCaptor<PlayerFinished> captor = ArgumentCaptor.forClass(PlayerFinished.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(Colour.RED, captor.getValue().colour());
        assertEquals(1, captor.getValue().place());
    }

    @Test
    @DisplayName("a capturing move does not grant a bonus roll (T-2 isn't active until phase 4b)")
    void captureDoesNotGrantBonusRoll() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(13));
        when(dice.roll()).thenReturn(3); // lands exactly on the opponent, non-six

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll();
    }
}
