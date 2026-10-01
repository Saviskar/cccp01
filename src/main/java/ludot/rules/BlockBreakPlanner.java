package ludot.rules;

import ludot.board.BoardState;
import ludot.board.BoardTopology;
import ludot.board.Piece;
import ludot.domain.Colour;
import ludot.domain.Direction;
import ludot.domain.PieceId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * T-6/A-22: snapshots which blocks a colour owns at the moment a third consecutive six lands, and
 * how each must break — before any member actually moves. Blocks are derived once, up front, so a
 * leaving member that happens to land on another of the colour's own blocks during execution can
 * never be mistaken for a newly discovered block or a block of a different size.
 */
public final class BlockBreakPlanner {

    // A-22: the 6 units shared equally among a block's leaving members.
    private static final int TOTAL_BREAK_UNITS = 6;

    public List<BlockBreak> plan(Colour colour, BoardState board, BoardTopology topology) {
        List<BlockBreak> breaks = new ArrayList<>();
        for (int cell : board.blockCellsOf(colour)) {
            breaks.add(planBreak(colour, cell, board, topology));
        }
        return breaks;
    }

    private BlockBreak planBreak(Colour colour, int cell, BoardState board, BoardTopology topology) {
        List<PieceId> members = board.piecesAt(cell);
        PieceId staying = farthestMember(members, colour, cell, board, topology);
        List<PieceId> leaving = members.stream()
                .filter(id -> !id.equals(staying))
                .sorted(Comparator.comparingInt(PieceId::number)) // A-40-style determinism
                .toList();
        int unitsEach = TOTAL_BREAK_UNITS / leaving.size(); // exact for block sizes 2-4 (A-22)
        return new BlockBreak(cell, staying, leaving, unitsEach);
    }

    // A-22: the farthest-from-home member stays, tie broken to the lowest piece number (contrast
    // A-17's block-direction tie-break, which goes to clockwise).
    private PieceId farthestMember(
            List<PieceId> members, Colour colour, int cell, BoardState board, BoardTopology topology) {
        return members.stream()
                .max(Comparator.<PieceId>comparingInt(id -> distanceFromHome(id, colour, cell, board, topology))
                        .thenComparing(Comparator.comparingInt(PieceId::number).reversed()))
                .orElseThrow();
    }

    private int distanceFromHome(PieceId id, Colour colour, int cell, BoardState board, BoardTopology topology) {
        Piece piece = board.piece(id);
        Direction direction = piece.originalDirection().orElseThrow();
        return topology.distanceFromHome(colour, direction, cell, piece.ccwApproachCrossings());
    }
}
