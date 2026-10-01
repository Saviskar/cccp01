package ludot.moves;

import ludot.board.BoardState;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import ludot.domain.Position;
import ludot.events.BlockMoved;
import ludot.events.EventBus;
import ludot.events.GameEventListener;
import ludot.random.Coin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockMoveTest {

    private static final Coin UNUSED_COIN = () -> Direction.CLOCKWISE;

    @Mock
    private GameEventListener listener;

    @Mock
    private LandingHandler landingHandler;

    @Mock
    private MysteryHandler mysteryHandler;

    private BoardState board;
    private EventBus events;

    @BeforeEach
    void setUp() {
        board = new BoardState();
        events = new EventBus();
        events.subscribe(listener);
    }

    @Test
    @DisplayName("T-4: moves every member to the destination")
    void t4_movesEveryMemberToDestination() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.moveTo(member2, new OnTrack(10));
        Move move = new BlockMove(
                List.of(member1, member2), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false,
                false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new OnTrack(13), board.piece(member1).position());
        assertEquals(new OnTrack(13), board.piece(member2).position());
    }

    @Test
    @DisplayName("A-08: records an Approach crossing for every member when crossesApproachWithoutEntering")
    void a08_recordsApproachCrossingForEveryMember() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(26));
        board.moveTo(member2, new OnTrack(26));
        Move move = new BlockMove(
                List.of(member1, member2), new OnTrack(26), new OnTrack(23), 6, 3, Direction.COUNTERCLOCKWISE, false,
                true);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(1, board.piece(member1).ccwApproachCrossings());
        assertEquals(1, board.piece(member2).ccwApproachCrossings());
    }

    @Test
    @DisplayName("a non-capturing block move never calls the landing handler")
    void nonCapturingBlockMoveDoesNotCallLandingHandler() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.moveTo(member2, new OnTrack(10));
        Move move = new BlockMove(
                List.of(member1, member2), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false,
                false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(landingHandler);
    }

    @Test
    @DisplayName("A-29: a block move never calls the mystery handler")
    void a29_neverCallsMysteryHandler() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.moveTo(member2, new OnTrack(10));
        Move move = new BlockMove(
                List.of(member1, member2), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false,
                false);

        move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        verifyNoInteractions(mysteryHandler);
    }

    @Test
    @DisplayName("A-46/A-51: a capturing block move publishes BlockMoved, then delegates to the landing handler")
    void a46_a51_capturingBlockMovePublishesThenDelegates() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.moveTo(member2, new OnTrack(10));
        Position destination = new OnTrack(13);
        List<PieceId> members = List.of(member1, member2);
        when(landingHandler.resolveLanding(members, destination, board, events))
                .thenReturn(new LandingResult(true, Optional.of(new PieceId(Colour.GREEN, 1))));
        Move move = new BlockMove(members, new OnTrack(10), destination, 6, 3, Direction.CLOCKWISE, true, false);

        MoveResult result =
                move.execute(new MoveContext(board, events, landingHandler, UNUSED_COIN, mysteryHandler));

        assertEquals(new MoveResult(true), result);
        InOrder order = inOrder(listener, landingHandler);
        order.verify(listener).onEvent(any(BlockMoved.class));
        order.verify(landingHandler).resolveLanding(members, destination, board, events);
    }

    @Test
    @DisplayName("A-29: a block move never lands on the mystery cell")
    void a29_neverLandsOnMystery() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        Move move = new BlockMove(
                List.of(member1, member2), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false,
                false);

        assertFalse(move.landsOnMystery());
    }

    @Test
    @DisplayName("a block move still forms a block and never breaks one")
    void formsBlockTrueBreaksBlockFalse() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        Move move = new BlockMove(
                List.of(member1, member2), new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false,
                false);

        assertTrue(move.formsBlock());
        assertFalse(move.breaksBlock());
    }

    @Test
    @DisplayName("defensively copies the piece id list so later mutation of the source list has no effect")
    void defensivelyCopiesPieceIdList() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        List<PieceId> source = new ArrayList<>(List.of(member1, member2));
        Move move = new BlockMove(
                source, new OnTrack(10), new OnTrack(13), 6, 3, Direction.CLOCKWISE, false, false);

        source.add(new PieceId(Colour.RED, 3));

        assertEquals(List.of(member1, member2), move.pieceIds());
    }
}
