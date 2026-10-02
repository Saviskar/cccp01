package ludot.engine;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.board.GameView;
import ludot.domain.AtHome;
import ludot.domain.Briefing;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.InBase;
import ludot.domain.InHomeStraight;
import ludot.domain.MysteryOutcomeKind;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.BlockadeBroken;
import ludot.events.BriefingStreakTriggered;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.events.NoLegalMove;
import ludot.events.PieceBlocked;
import ludot.events.PiecePartiallyMoved;
import ludot.events.PlayerFinished;
import ludot.events.ThirdSixIgnored;
import ludot.events.ThrowIgnoredAfterBlock;
import ludot.players.FirstLegalMoveStrategy;
import ludot.players.PlayerStrategy;
import ludot.random.Dice;
import ludot.random.RandomPicker;
import ludot.rules.BlockBreakPlanner;
import ludot.rules.LandingResolver;
import ludot.rules.MoveGenerator;
import ludot.rules.MovementCalculator;
import ludot.rules.MysteryOutcome;
import ludot.rules.MysteryOutcomeFactory;
import ludot.rules.MysteryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnControllerTest {

    @Mock
    private Dice dice;

    @Mock
    private GameEventListener listener;

    @Mock
    private RandomPicker mysteryPicker;

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
        // Plumbing only: none of these tests reach EnterFromBase on every roll sequence, so a
        // Mockito mock here would trip strict-stubs' UnnecessaryStubbingException; a lambda is
        // always "used" correctly whether or not a base entry occurs. mysteryPicker is a real
        // MysteryResolver's randomness source; no scenario here sets up a mystery cell unless the
        // test stubs mysteryPicker itself (A-54 below), so it otherwise goes uninvoked.
        MysteryResolver mysteryResolver =
                new MysteryResolver(new MysteryOutcomeFactory(), mysteryPicker, topology, new LandingResolver());
        controller = new TurnController(
                dice, () -> Direction.CLOCKWISE, moveGenerator, topology, events, new LandingResolver(),
                new BlockBreakPlanner(), mysteryResolver);
        player = new Player(Colour.RED, new FirstLegalMoveStrategy());
    }

    private GameView view() {
        return board;
    }

    // A-28: spawns the mystery cell at exactly `index`, assuming a piece is already on the track.
    private void placeMysteryCellAt(int index) {
        // doAnswer (not when/thenAnswer): safe to re-stub later without calling through to this
        // answer during registration, since anyList()'s dummy argument is an empty list.
        doAnswer(inv -> {
            List<?> options = inv.getArgument(0);
            if (!options.isEmpty() && options.get(0) instanceof Integer) {
                return index;
            }
            // A-30: the outcome draw, when stubbed generically alongside the cell pick -- tests
            // that care which outcome is drawn stub this call specifically instead.
            return options.isEmpty() ? null : options.get(0);
        }).when(mysteryPicker).pick(anyList());
        board.tickMysteryCell(mysteryPicker);
        board.tickMysteryCell(mysteryPicker);
        board.tickMysteryCell(mysteryPicker);
    }

    // T-6 test helper: always picks a block move (over an individual step) when one is legal,
    // so a pre-placed block survives the two "ordinary" six-rolls that precede a third
    // consecutive six intact, letting the tests below exercise A-22's break in isolation.
    private static final PlayerStrategy PREFER_BLOCK_MOVE =
            (moves, view) -> moves.stream().filter(m -> m.pieceIds().size() > 1).findFirst().orElse(moves.get(0));

    // T-6 test helper: on its Nth call, picks the block move whose origin is cellsInPriorityOrder[N].
    // Lets a test control exactly which of several blocks relocates on each of the two ordinary
    // six-rolls, so their post-relocation cells can be chosen deliberately (e.g. to make one
    // block's eventual leaver land on another block's cell, per the snapshot test below).
    private static PlayerStrategy pickBlockMoveFromCell(int... cellsInPriorityOrder) {
        List<Integer> order = new ArrayList<>();
        for (int cell : cellsInPriorityOrder) {
            order.add(cell);
        }
        int[] callIndex = {0};
        return (moves, view) -> {
            int targetCell = order.get(callIndex[0]++);
            return moves.stream()
                    .filter(m -> m.pieceIds().size() > 1 && m.origin() instanceof OnTrack(int idx) && idx == targetCell)
                    .findFirst()
                    .orElseThrow();
        };
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
        board.recordCapture(last); // A-07: eligible to enter the home straight
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
    @DisplayName("A-23/T-2: a capture grants a bonus roll")
    void a23_captureGrantsBonusRoll() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(13));
        when(dice.roll()).thenReturn(3, 2); // lands exactly on the opponent, non-six, then a bonus roll

        controller.playTurn(player, board, standings, view());

        verify(dice, times(2)).roll();
        assertEquals(new InBase(), board.piece(opponent).position());
    }

    @Test
    @DisplayName("A-23: a six that also captures does not stack into two bonus rolls")
    void a23_sixAndCaptureDoNotStack() {
        // The other three Red pieces are already Home so a six can't also offer them
        // an EnterFromBase move, which would let FirstLegalMoveStrategy pick that
        // instead of the capturing move.
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 3), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 4), new AtHome());
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(16));
        when(dice.roll()).thenReturn(6, 3); // captures on the six, then a non-capturing, non-six bonus roll

        controller.playTurn(player, board, standings, view());

        verify(dice, times(2)).roll(); // not three: the six and the capture share one bonus roll
        assertEquals(new InBase(), board.piece(opponent).position());
    }

    @Test
    @DisplayName("a six that captures on entry from base grants exactly one bonus roll, not two "
            + "(A-23 non-stacking via EnterFromBase; A-25 capture on X)")
    void a23_a25_baseEntryCaptureOnSixGrantsOnlyOneBonus() {
        // Entering from base always requires a six (Rule 2), so the capturing roll here is
        // necessarily also a six. times(2) alone can't distinguish "the capture granted a bonus"
        // from "the six granted a bonus" (A-47) — this test instead proves non-stacking: a
        // captured base entry still yields exactly one bonus roll, not two.
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(opponent, new OnTrack(topology.xIndex(Colour.RED)));
        when(dice.roll()).thenReturn(6, 2); // captures entering from base, then a non-six bonus roll

        controller.playTurn(player, board, standings, view());

        verify(dice, times(2)).roll();
        assertEquals(new InBase(), board.piece(opponent).position());
    }

    @Test
    @DisplayName("A-24: a bonus roll earned from a capture counts as a normal roll in the six streak")
    void a24_captureBonusRollCountsAsNormalRollInSixStreak() {
        PieceId mover = new PieceId(Colour.RED, 1);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(13));
        when(dice.roll()).thenReturn(3, 6, 6, 6); // capture bonus, then three consecutive sixes

        controller.playTurn(player, board, standings, view());

        verify(dice, times(4)).roll();
        verify(listener).onEvent(any(ThirdSixIgnored.class));
        assertEquals(new InBase(), board.piece(opponent).position());
    }

    @Test
    @DisplayName("A-48: a dead-end obstruction publishes PieceBlocked then ThrowIgnoredAfterBlock, never NoLegalMove")
    void a48_deadEndObstructionPublishesBlockedThenIgnoringThrow() {
        PieceId mover = new PieceId(Colour.RED, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(11));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(11));
        when(dice.roll()).thenReturn(4);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll();
        InOrder order = inOrder(listener);
        order.verify(listener).onEvent(any(PieceBlocked.class));
        order.verify(listener).onEvent(any(ThrowIgnoredAfterBlock.class));
        verify(listener, never()).onEvent(any(NoLegalMove.class));
    }

    @Test
    @DisplayName("A-47 exception: a six that produces only a dead-end obstruction ends the turn without a bonus roll")
    void a47_sixWithDeadEndObstructionEndsTurnWithoutBonus() {
        PieceId mover = new PieceId(Colour.RED, 1);
        board.moveTo(mover, new OnTrack(10));
        board.assignDirection(mover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(11));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(11));
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 3), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 4), new AtHome());
        when(dice.roll()).thenReturn(6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll(); // A-47's exception: no bonus roll despite the six
        verify(listener).onEvent(any(ThrowIgnoredAfterBlock.class));
    }

    @Test
    @DisplayName("A-48: a mixed dead-end and partial-move turn reports only the executed partial mover")
    void a48_mixedDeadEndAndPartialReportsOnlyExecutedMover() {
        PieceId deadEndMover = new PieceId(Colour.RED, 1);
        PieceId partialMover = new PieceId(Colour.RED, 2);
        board.moveTo(deadEndMover, new OnTrack(20));
        board.assignDirection(deadEndMover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(21));
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(21));
        board.moveTo(partialMover, new OnTrack(10));
        board.assignDirection(partialMover, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 3), new OnTrack(12));
        board.moveTo(new PieceId(Colour.GREEN, 4), new OnTrack(12));
        when(dice.roll()).thenReturn(4);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(1)).roll();
        verify(listener, never()).onEvent(any(ThrowIgnoredAfterBlock.class));
        ArgumentCaptor<PieceBlocked> blockedCaptor = ArgumentCaptor.forClass(PieceBlocked.class);
        verify(listener).onEvent(blockedCaptor.capture());
        assertEquals(partialMover, blockedCaptor.getValue().pieceId());
        verify(listener).onEvent(any(PiecePartiallyMoved.class));
    }

    @Test
    @DisplayName("T-6/A-22: a third consecutive six breaks a block of two, moving the remaining member 6 units")
    void t6_a22_threeSixesBreaksBlockOfTwo_remainingMemberMovesSixUnits() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 3);
        PieceId leaving = new PieceId(Colour.RED, 4);
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(10));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE); // farthest: no crossing yet (A-13)
        board.moveTo(leaving, new OnTrack(10));
        board.assignDirection(leaving, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll(); // A-22: the turn has ended, no bonus roll
        assertEquals(new OnTrack(12), board.piece(absorber).position()); // absorbed both ordinary sixes
        assertEquals(new OnTrack(10), board.piece(staying).position());
        assertEquals(new OnTrack(16), board.piece(leaving).position());
        ArgumentCaptor<BlockadeBroken> captor = ArgumentCaptor.forClass(BlockadeBroken.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(Colour.RED, captor.getValue().colour());
        assertEquals(10, captor.getValue().cell());
        assertEquals(List.of(staying), captor.getValue().staying());
        assertEquals(List.of(leaving), captor.getValue().leaving());
        assertEquals(6, captor.getValue().unitsEach());
    }

    @Test
    @DisplayName("T-6/A-22: a third consecutive six breaks a block of three, splitting 6 units as 3 and 3")
    void t6_a22_threeSixesBreaksBlockOfThree_splitsSixUnitsAsThreeAndThree() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 2);
        PieceId leaving1 = new PieceId(Colour.RED, 3);
        PieceId leaving2 = new PieceId(Colour.RED, 4);
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(10));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving1, new OnTrack(10));
        board.assignDirection(leaving1, Direction.CLOCKWISE);
        board.moveTo(leaving2, new OnTrack(10));
        board.assignDirection(leaving2, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll();
        assertEquals(new OnTrack(10), board.piece(staying).position());
        assertEquals(new OnTrack(13), board.piece(leaving1).position());
        assertEquals(new OnTrack(13), board.piece(leaving2).position());
    }

    @Test
    @DisplayName("T-6/A-22: a third consecutive six breaks a block of four, splitting 6 units as 2, 2 and 2")
    void t6_a22_threeSixesBreaksBlockOfFour_splitsSixUnitsAsTwoEach() {
        PieceId staying = new PieceId(Colour.RED, 1);
        PieceId leaving1 = new PieceId(Colour.RED, 2);
        PieceId leaving2 = new PieceId(Colour.RED, 3);
        PieceId leaving3 = new PieceId(Colour.RED, 4);
        board.moveTo(staying, new OnTrack(50));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving1, new OnTrack(50));
        board.assignDirection(leaving1, Direction.CLOCKWISE);
        board.moveTo(leaving2, new OnTrack(50));
        board.assignDirection(leaving2, Direction.CLOCKWISE);
        board.moveTo(leaving3, new OnTrack(50));
        board.assignDirection(leaving3, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 6, 6);
        Player blockPlayer = new Player(Colour.RED, PREFER_BLOCK_MOVE);

        controller.playTurn(blockPlayer, board, standings, view());

        verify(dice, times(3)).roll();
        assertEquals(new OnTrack(48), board.piece(staying).position());
        assertEquals(new OnTrack(50), board.piece(leaving1).position());
        assertEquals(new OnTrack(50), board.piece(leaving2).position());
        assertEquals(new OnTrack(50), board.piece(leaving3).position());
    }

    @Test
    @DisplayName("T-6/A-22: a colour owning two blocks breaks both deterministically")
    void t6_a22_multipleBlocksBothBreak_processedDeterministically() {
        PieceId aStays = new PieceId(Colour.RED, 1);
        PieceId aLeaves = new PieceId(Colour.RED, 2);
        PieceId bStays = new PieceId(Colour.RED, 3);
        PieceId bLeaves = new PieceId(Colour.RED, 4);
        board.moveTo(aStays, new OnTrack(10));
        board.assignDirection(aStays, Direction.COUNTERCLOCKWISE);
        board.moveTo(aLeaves, new OnTrack(10));
        board.assignDirection(aLeaves, Direction.CLOCKWISE);
        board.moveTo(bStays, new OnTrack(30));
        board.assignDirection(bStays, Direction.COUNTERCLOCKWISE);
        board.moveTo(bLeaves, new OnTrack(30));
        board.assignDirection(bLeaves, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 6, 6);
        Player blockPlayer = new Player(Colour.RED, PREFER_BLOCK_MOVE);

        controller.playTurn(blockPlayer, board, standings, view());

        verify(dice, times(3)).roll();
        // Block A (lowest piece number) relocates on both ordinary sixes (10 -> 7 -> 4), then breaks;
        // block B is never the preferred move, so it never moves until its own break.
        assertEquals(new OnTrack(4), board.piece(aStays).position());
        assertEquals(new OnTrack(10), board.piece(aLeaves).position());
        assertEquals(new OnTrack(30), board.piece(bStays).position());
        assertEquals(new OnTrack(36), board.piece(bLeaves).position());
    }

    @Test
    @DisplayName("T-6/A-22: a capture during a forced break counts but grants no bonus roll")
    void t6_a22_captureDuringBreakCountsButGrantsNoBonusRoll() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 3);
        PieceId leaving = new PieceId(Colour.RED, 4);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(15));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving, new OnTrack(15));
        board.assignDirection(leaving, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(21)); // exactly 6 cw cells from 15
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll(); // no bonus roll despite the capture (A-22)
        assertEquals(new OnTrack(15), board.piece(staying).position());
        assertEquals(new OnTrack(21), board.piece(leaving).position());
        assertEquals(1, board.piece(leaving).captureCount());
        assertEquals(new InBase(), board.piece(opponent).position());
    }

    @Test
    @DisplayName("A-52: a forced break move obstructed with room publishes PieceBlocked then PiecePartiallyMoved")
    void t6_a52_obstructedBreakMovePublishesBlockedThenPartialMove() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 3);
        PieceId leaving = new PieceId(Colour.RED, 4);
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(15));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving, new OnTrack(15));
        board.assignDirection(leaving, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(19)); // 4 cells into the 6-unit path
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(19));
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll();
        assertEquals(new OnTrack(18), board.piece(leaving).position());
        assertEquals(new OnTrack(15), board.piece(staying).position());
        InOrder order = inOrder(listener);
        order.verify(listener).onEvent(any(PieceBlocked.class));
        order.verify(listener).onEvent(any(PiecePartiallyMoved.class));
        verify(listener, never()).onEvent(any(ThrowIgnoredAfterBlock.class)); // A-52: a forced break is not a throw
    }

    @Test
    @DisplayName("A-52: an adjacent obstruction publishes only the blocked fact, never ignoring the throw")
    void t6_a52_adjacentObstructionPublishesOnlyBlockedFact_noThrowIgnoredEvent() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 3);
        PieceId leaving = new PieceId(Colour.RED, 4);
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(15));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving, new OnTrack(15));
        board.assignDirection(leaving, Direction.CLOCKWISE);
        board.moveTo(new PieceId(Colour.GREEN, 1), new OnTrack(16)); // immediately adjacent
        board.moveTo(new PieceId(Colour.GREEN, 2), new OnTrack(16));
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll();
        assertEquals(new OnTrack(15), board.piece(leaving).position()); // stays put (A-52)
        verify(listener).onEvent(any(PieceBlocked.class));
        verify(listener, never()).onEvent(any(PiecePartiallyMoved.class));
        verify(listener, never()).onEvent(any(ThrowIgnoredAfterBlock.class));
    }

    @Test
    @DisplayName("Rule 4 regression: with no block owned, a third consecutive six is still just ignored")
    void t6_rule4_noBlockOwned_thirdSixStillJustIgnored() {
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll();
        verify(listener).onEvent(any(ThirdSixIgnored.class));
        verify(listener, never()).onEvent(any(BlockadeBroken.class));
    }

    @Test
    @DisplayName("T-6/A-22: the break plan is snapshotted up front, so a leaver landing on another of the "
            + "colour's own blocks mid-sequence neither corrupts that block's own break nor moves twice")
    void t6_a22_blockBreakSnapshotIsFixedUpFront_leaverLandingOnAnotherBlockDoesNotCorruptIt() {
        PieceId aStays = new PieceId(Colour.RED, 1);
        PieceId aLeaves = new PieceId(Colour.RED, 2);
        PieceId bStays = new PieceId(Colour.RED, 3);
        PieceId bLeaves = new PieceId(Colour.RED, 4);
        board.moveTo(aStays, new OnTrack(10));
        board.assignDirection(aStays, Direction.COUNTERCLOCKWISE);
        board.moveTo(aLeaves, new OnTrack(10));
        board.assignDirection(aLeaves, Direction.CLOCKWISE);
        board.moveTo(bStays, new OnTrack(16));
        board.assignDirection(bStays, Direction.COUNTERCLOCKWISE);
        board.moveTo(bLeaves, new OnTrack(16));
        board.assignDirection(bLeaves, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 6, 6);
        // Roll 1 relocates block A (10 -> 7); roll 2 relocates block B (16 -> 13). A's leaver then
        // breaks 6 units clockwise from 7, landing exactly on 13 -- block B's cell at that moment.
        Player blockPlayer = new Player(Colour.RED, pickBlockMoveFromCell(10, 16));

        controller.playTurn(blockPlayer, board, standings, view());

        verify(dice, times(3)).roll();
        assertEquals(new OnTrack(7), board.piece(aStays).position());
        assertEquals(new OnTrack(13), board.piece(aLeaves).position()); // lands on block B's cell, moved once
        assertEquals(new OnTrack(13), board.piece(bStays).position()); // B still breaks as its own 2-block...
        assertEquals(new OnTrack(19), board.piece(bLeaves).position()); // ...6 units, not re-split for a 3rd member
    }

    @Test
    @DisplayName("A-54: a forced-break member landing on the mystery cell captures via teleport, "
            + "but still grants no bonus roll")
    void a54_forcedBreakMysteryCaptureGrantsNoBonusRoll() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 3);
        PieceId leaving = new PieceId(Colour.RED, 4);
        PieceId opponent = new PieceId(Colour.GREEN, 1);
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(15));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving, new OnTrack(15));
        board.assignDirection(leaving, Direction.CLOCKWISE);
        board.moveTo(opponent, new OnTrack(topology.xIndex(Colour.RED))); // where the teleport will capture
        placeMysteryCellAt(21); // leaving's post-break landing cell (15 + 6 clockwise)
        // doAnswer (not when/thenAnswer): re-stubbing the same mock+matcher via when() would call
        // through to the still-active stub above during registration itself, since anyList()'s
        // dummy argument is an empty list and that stub indexes into it.
        doAnswer(inv -> {
            List<MysteryOutcome> options = inv.getArgument(0);
            return options.stream().filter(o -> o.kind() == MysteryOutcomeKind.X).findFirst().orElseThrow();
        }).when(mysteryPicker).pick(anyList());
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll(); // A-22/A-54: the turn has ended, no bonus roll despite the capture
        assertEquals(new OnTrack(topology.xIndex(Colour.RED)), board.piece(leaving).position());
        assertEquals(new InBase(), board.piece(opponent).position());
        assertEquals(1, board.piece(leaving).captureCount());
    }

    @Test
    @DisplayName("t13_a33: a Beta-restricted piece offers no move at all")
    void t13_a33_restrictedPieceOffersNoMove() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Briefing());
        when(dice.roll()).thenReturn(4);

        controller.playTurn(player, board, standings, view());

        assertEquals(new OnTrack(10), board.piece(id).position());
        verify(listener).onEvent(any(NoLegalMove.class));
    }

    @Test
    @DisplayName("t13_a56: three consecutive 3s across separate turns send a Beta-restricted piece to base")
    void t13_a56_threeThreesSendsRestrictedPieceToBase() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Briefing());
        when(dice.roll()).thenReturn(3); // every roll returns 3; each turn consumes exactly one (non-six, no capture)

        controller.playTurn(player, board, standings, view()); // 1st three
        controller.playTurn(player, board, standings, view()); // 2nd three
        controller.playTurn(player, board, standings, view()); // 3rd three -- triggers

        assertEquals(new InBase(), board.piece(id).position());
        verify(listener).onEvent(any(BriefingStreakTriggered.class));
    }

    @Test
    @DisplayName("t13_a56: releases every currently Beta-restricted piece of the colour, not just one")
    void t13_a56_threeThreesReleasesEveryRestrictedPieceOfTheColour() {
        PieceId restricted1 = new PieceId(Colour.RED, 1);
        PieceId restricted2 = new PieceId(Colour.RED, 2);
        board.moveTo(restricted1, new OnTrack(10));
        board.assignDirection(restricted1, Direction.CLOCKWISE);
        board.applyEffect(restricted1, new Briefing());
        board.moveTo(restricted2, new OnTrack(20));
        board.assignDirection(restricted2, Direction.CLOCKWISE);
        board.applyEffect(restricted2, new Briefing());
        when(dice.roll()).thenReturn(3);

        controller.playTurn(player, board, standings, view());
        controller.playTurn(player, board, standings, view());
        controller.playTurn(player, board, standings, view());

        assertEquals(new InBase(), board.piece(restricted1).position());
        assertEquals(new InBase(), board.piece(restricted2).position());
        verify(listener, times(2)).onEvent(any(BriefingStreakTriggered.class));
    }

    @Test
    @DisplayName("t13_a56: the streak only counts while a piece is restricted -- rolls before restriction "
            + "don't carry over")
    void t13_a56_streakOnlyCountsWhileAPieceIsRestricted() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(3);

        controller.playTurn(player, board, standings, view()); // 1st three, unrestricted: doesn't count
        controller.playTurn(player, board, standings, view()); // 2nd three, unrestricted: doesn't count
        board.applyEffect(id, new Briefing());
        Position afterRestriction = board.piece(id).position();

        controller.playTurn(player, board, standings, view()); // 1st three while restricted
        controller.playTurn(player, board, standings, view()); // 2nd three while restricted

        assertEquals(afterRestriction, board.piece(id).position()); // not yet released
        verify(listener, never()).onEvent(any(BriefingStreakTriggered.class));

        controller.playTurn(player, board, standings, view()); // 3rd three while restricted -- triggers

        assertEquals(new InBase(), board.piece(id).position());
        verify(listener).onEvent(any(BriefingStreakTriggered.class));
    }

    @Test
    @DisplayName("t13_a56: the streak resets to 0 after triggering -- a later restricted piece needs a fresh "
            + "three-in-a-row")
    void t13_a56_streakResetsAfterTriggering() {
        PieceId first = new PieceId(Colour.RED, 1);
        PieceId second = new PieceId(Colour.RED, 2);
        board.moveTo(first, new OnTrack(10));
        board.assignDirection(first, Direction.CLOCKWISE);
        board.applyEffect(first, new Briefing());
        when(dice.roll()).thenReturn(3);

        controller.playTurn(player, board, standings, view());
        controller.playTurn(player, board, standings, view());
        controller.playTurn(player, board, standings, view()); // triggers: first released

        assertEquals(new InBase(), board.piece(first).position());

        board.moveTo(second, new OnTrack(20));
        board.assignDirection(second, Direction.CLOCKWISE);
        board.applyEffect(second, new Briefing());

        controller.playTurn(player, board, standings, view()); // 1st three for second's streak
        controller.playTurn(player, board, standings, view()); // 2nd three for second's streak

        assertEquals(new OnTrack(20), board.piece(second).position()); // not yet released after only 2

        controller.playTurn(player, board, standings, view()); // 3rd three -- releases second

        assertEquals(new InBase(), board.piece(second).position());
    }

    @Test
    @DisplayName("t13_a33: a non-3 roll resets an in-progress streak")
    void t13_a33_nonThreeRollResetsAnInProgressStreak() {
        PieceId id = new PieceId(Colour.RED, 1);
        board.moveTo(id, new OnTrack(10));
        board.assignDirection(id, Direction.CLOCKWISE);
        board.applyEffect(id, new Briefing());
        when(dice.roll()).thenReturn(3, 3, 4, 3, 3, 3);

        for (int i = 0; i < 5; i++) {
            controller.playTurn(player, board, standings, view());
        }
        assertEquals(new OnTrack(10), board.piece(id).position()); // the "4" reset the streak; not yet released

        controller.playTurn(player, board, standings, view()); // completes a fresh three-in-a-row

        assertEquals(new InBase(), board.piece(id).position());
    }

    @Test
    @DisplayName("t13_a57: a third consecutive six with every block member Beta-restricted breaks nothing")
    void t13_a57_allRestrictedBlockPublishesNoBlockadeBroken() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(new PieceId(Colour.RED, 3), new AtHome());
        board.moveTo(new PieceId(Colour.RED, 4), new AtHome());
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.applyEffect(member1, new Briefing());
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.applyEffect(member2, new Briefing());
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll();
        verify(listener, never()).onEvent(any(BlockadeBroken.class));
        assertEquals(new OnTrack(10), board.piece(member1).position());
        assertEquals(new OnTrack(10), board.piece(member2).position());
    }

    @Test
    @DisplayName("t13_a57: a third consecutive six partially breaks a block with one Beta-restricted "
            + "member, which stays while the other leaves all 6 units")
    void t13_a57_partialBreakRestrictedMemberStaysOtherLeavesSixUnits() {
        PieceId absorber = new PieceId(Colour.RED, 1);
        PieceId staying = new PieceId(Colour.RED, 3);
        PieceId leaving = new PieceId(Colour.RED, 4);
        board.moveTo(new PieceId(Colour.RED, 2), new AtHome());
        board.moveTo(absorber, new OnTrack(0));
        board.assignDirection(absorber, Direction.CLOCKWISE);
        board.moveTo(staying, new OnTrack(10));
        board.assignDirection(staying, Direction.CLOCKWISE);
        board.applyEffect(staying, new Briefing());
        board.moveTo(leaving, new OnTrack(10));
        board.assignDirection(leaving, Direction.CLOCKWISE);
        when(dice.roll()).thenReturn(6, 6, 6);

        controller.playTurn(player, board, standings, view());

        verify(dice, times(3)).roll(); // A-22: the turn has ended, no bonus roll
        assertEquals(new OnTrack(10), board.piece(staying).position());
        assertEquals(new OnTrack(16), board.piece(leaving).position());
        ArgumentCaptor<BlockadeBroken> captor = ArgumentCaptor.forClass(BlockadeBroken.class);
        verify(listener).onEvent(captor.capture());
        assertEquals(List.of(staying), captor.getValue().staying());
        assertEquals(List.of(leaving), captor.getValue().leaving());
        assertEquals(6, captor.getValue().unitsEach());
    }
}
