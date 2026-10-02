package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.domain.Briefing;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.OnTrack;
import ludot.domain.PieceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockBreakPlannerTest {

    private final BoardTopology topology = new BoardTopology();
    private final BlockBreakPlanner planner = new BlockBreakPlanner();
    private BoardState board;

    @BeforeEach
    void setUp() {
        board = new BoardState();
    }

    @Test
    @DisplayName("T-6/A-22: a colour with no blocks gets an empty plan")
    void t6_a22_noBlocksOwned_returnsEmptyPlan() {
        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertTrue(plan.isEmpty());
    }

    @Test
    @DisplayName("T-6/A-22: a block of two leaves one member moving all 6 units")
    void t6_a22_blockOfTwo_oneLeaverSixUnits() {
        PieceId staying = new PieceId(Colour.RED, 1);
        PieceId leaving = new PieceId(Colour.RED, 2);
        board.moveTo(staying, new OnTrack(10));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE); // farther from home (A-13): no crossing yet
        board.moveTo(leaving, new OnTrack(10));
        board.assignDirection(leaving, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        BlockBreak blockBreak = plan.get(0);
        assertEquals(10, blockBreak.cell());
        assertEquals(List.of(staying), blockBreak.staying());
        assertEquals(List.of(leaving), blockBreak.leaving());
        assertEquals(6, blockBreak.unitsEach());
    }

    @Test
    @DisplayName("T-6/A-22: a block of three splits 6 units as 3 and 3")
    void t6_a22_blockOfThree_twoLeaversThreeUnitsEach() {
        PieceId staying = new PieceId(Colour.RED, 1);
        PieceId leaving1 = new PieceId(Colour.RED, 2);
        PieceId leaving2 = new PieceId(Colour.RED, 3);
        board.moveTo(staying, new OnTrack(10));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE); // farthest: no crossing yet, full extra lap
        board.moveTo(leaving1, new OnTrack(10));
        board.assignDirection(leaving1, Direction.CLOCKWISE);
        board.moveTo(leaving2, new OnTrack(10));
        board.assignDirection(leaving2, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        BlockBreak blockBreak = plan.get(0);
        assertEquals(List.of(staying), blockBreak.staying());
        assertEquals(List.of(leaving1, leaving2), blockBreak.leaving());
        assertEquals(3, blockBreak.unitsEach());
    }

    @Test
    @DisplayName("T-6/A-22: a block of four splits 6 units as 2, 2 and 2")
    void t6_a22_blockOfFour_threeLeaversTwoUnitsEach() {
        PieceId staying = new PieceId(Colour.RED, 1);
        PieceId leaving1 = new PieceId(Colour.RED, 2);
        PieceId leaving2 = new PieceId(Colour.RED, 3);
        PieceId leaving3 = new PieceId(Colour.RED, 4);
        board.moveTo(staying, new OnTrack(10));
        board.assignDirection(staying, Direction.COUNTERCLOCKWISE);
        board.moveTo(leaving1, new OnTrack(10));
        board.assignDirection(leaving1, Direction.CLOCKWISE);
        board.moveTo(leaving2, new OnTrack(10));
        board.assignDirection(leaving2, Direction.CLOCKWISE);
        board.moveTo(leaving3, new OnTrack(10));
        board.assignDirection(leaving3, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        BlockBreak blockBreak = plan.get(0);
        assertEquals(List.of(staying), blockBreak.staying());
        assertEquals(List.of(leaving1, leaving2, leaving3), blockBreak.leaving());
        assertEquals(2, blockBreak.unitsEach());
    }

    @Test
    @DisplayName("A-22: an equal-distance tie for farthest is broken to the lowest piece number")
    void t6_a22_farthestMemberStays_tieBrokenByLowestPieceNumber() {
        PieceId lower = new PieceId(Colour.RED, 1);
        PieceId higher = new PieceId(Colour.RED, 2);
        board.moveTo(lower, new OnTrack(10));
        board.assignDirection(lower, Direction.CLOCKWISE);
        board.moveTo(higher, new OnTrack(10));
        board.assignDirection(higher, Direction.CLOCKWISE); // identical distance: same cell, same direction

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        assertEquals(List.of(lower), plan.get(0).staying());
        assertEquals(List.of(higher), plan.get(0).leaving());
    }

    @Test
    @DisplayName("T-6/A-22: a colour owning two blocks gets one independent plan per block")
    void t6_a22_multipleBlocksEachPlannedIndependently() {
        PieceId stayingA = new PieceId(Colour.RED, 1);
        PieceId leavingA = new PieceId(Colour.RED, 2);
        PieceId stayingB = new PieceId(Colour.RED, 3);
        PieceId leavingB = new PieceId(Colour.RED, 4);
        board.moveTo(stayingA, new OnTrack(10));
        board.assignDirection(stayingA, Direction.COUNTERCLOCKWISE);
        board.moveTo(leavingA, new OnTrack(10));
        board.assignDirection(leavingA, Direction.CLOCKWISE);
        board.moveTo(stayingB, new OnTrack(30));
        board.assignDirection(stayingB, Direction.COUNTERCLOCKWISE);
        board.moveTo(leavingB, new OnTrack(30));
        board.assignDirection(leavingB, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(2, plan.size());
        BlockBreak breakA = plan.stream().filter(b -> b.cell() == 10).findFirst().orElseThrow();
        BlockBreak breakB = plan.stream().filter(b -> b.cell() == 30).findFirst().orElseThrow();
        assertEquals(List.of(stayingA), breakA.staying());
        assertEquals(List.of(leavingA), breakA.leaving());
        assertEquals(List.of(stayingB), breakB.staying());
        assertEquals(List.of(leavingB), breakB.leaving());
    }

    @Test
    @DisplayName("t13_a57: a block of two with one Beta-restricted member — the restricted member "
            + "stays, the other leaves all 6 units")
    void t13_a57_blockOfTwoWithOneRestricted_restrictedStaysOtherLeavesSixUnits() {
        PieceId restricted = new PieceId(Colour.RED, 1);
        PieceId unrestricted = new PieceId(Colour.RED, 2);
        board.moveTo(restricted, new OnTrack(10));
        board.assignDirection(restricted, Direction.CLOCKWISE);
        board.applyEffect(restricted, new Briefing());
        board.moveTo(unrestricted, new OnTrack(10));
        board.assignDirection(unrestricted, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        BlockBreak blockBreak = plan.get(0);
        assertEquals(List.of(restricted), blockBreak.staying());
        assertEquals(List.of(unrestricted), blockBreak.leaving());
        assertEquals(6, blockBreak.unitsEach());
    }

    @Test
    @DisplayName("t13_a57: a block of three with one Beta-restricted member — the restricted member "
            + "stays, the other two split 6 units as 3 and 3")
    void t13_a57_blockOfThreeWithOneRestricted_restrictedStaysOtherTwoSplitThreeEach() {
        PieceId restricted = new PieceId(Colour.RED, 1);
        PieceId leaving1 = new PieceId(Colour.RED, 2);
        PieceId leaving2 = new PieceId(Colour.RED, 3);
        board.moveTo(restricted, new OnTrack(10));
        board.assignDirection(restricted, Direction.CLOCKWISE);
        board.applyEffect(restricted, new Briefing());
        board.moveTo(leaving1, new OnTrack(10));
        board.assignDirection(leaving1, Direction.CLOCKWISE);
        board.moveTo(leaving2, new OnTrack(10));
        board.assignDirection(leaving2, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        BlockBreak blockBreak = plan.get(0);
        assertEquals(List.of(restricted), blockBreak.staying());
        assertEquals(List.of(leaving1, leaving2), blockBreak.leaving());
        assertEquals(3, blockBreak.unitsEach());
    }

    @Test
    @DisplayName("t13_a57: a block where every member is Beta-restricted is omitted from the plan entirely")
    void t13_a57_allMembersRestricted_blockOmittedFromPlan() {
        PieceId member1 = new PieceId(Colour.RED, 1);
        PieceId member2 = new PieceId(Colour.RED, 2);
        board.moveTo(member1, new OnTrack(10));
        board.assignDirection(member1, Direction.CLOCKWISE);
        board.applyEffect(member1, new Briefing());
        board.moveTo(member2, new OnTrack(10));
        board.assignDirection(member2, Direction.CLOCKWISE);
        board.applyEffect(member2, new Briefing());

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertFalse(plan.stream().anyMatch(b -> b.cell() == 10));
        assertTrue(plan.isEmpty());
    }

    @Test
    @DisplayName("t13_a57: a block of four with two Beta-restricted members — both stay, the other two "
            + "split 6 units as 3 and 3")
    void t13_a57_blockOfFourWithTwoRestricted_bothStayOtherTwoSplitThreeEach() {
        PieceId restricted1 = new PieceId(Colour.RED, 1);
        PieceId restricted2 = new PieceId(Colour.RED, 2);
        PieceId leaving1 = new PieceId(Colour.RED, 3);
        PieceId leaving2 = new PieceId(Colour.RED, 4);
        board.moveTo(restricted1, new OnTrack(10));
        board.assignDirection(restricted1, Direction.CLOCKWISE);
        board.applyEffect(restricted1, new Briefing());
        board.moveTo(restricted2, new OnTrack(10));
        board.assignDirection(restricted2, Direction.CLOCKWISE);
        board.applyEffect(restricted2, new Briefing());
        board.moveTo(leaving1, new OnTrack(10));
        board.assignDirection(leaving1, Direction.CLOCKWISE);
        board.moveTo(leaving2, new OnTrack(10));
        board.assignDirection(leaving2, Direction.CLOCKWISE);

        List<BlockBreak> plan = planner.plan(Colour.RED, board, topology);

        assertEquals(1, plan.size());
        BlockBreak blockBreak = plan.get(0);
        assertEquals(List.of(restricted1, restricted2), blockBreak.staying());
        assertEquals(List.of(leaving1, leaving2), blockBreak.leaving());
        assertEquals(3, blockBreak.unitsEach());
    }
}
