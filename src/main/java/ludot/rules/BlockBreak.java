package ludot.rules;

import ludot.domain.PieceId;

import java.util.List;

/**
 * T-6/A-22: one block's break plan, snapshotted before any member moves — the cell it occupied,
 * the member staying behind, the members leaving (ascending piece number), and how many units
 * each leaving member moves.
 */
public record BlockBreak(int cell, PieceId staying, List<PieceId> leaving, int unitsEach) {

    public BlockBreak {
        leaving = List.copyOf(leaving);
    }
}
