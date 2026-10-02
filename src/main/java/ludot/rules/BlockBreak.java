package ludot.rules;

import ludot.domain.PieceId;

import java.util.List;

/**
 * T-6/A-22: one block's break plan, snapshotted before any member moves — the cell it occupied,
 * the member(s) staying behind, the members leaving (ascending piece number), and how many units
 * each leaving member moves. Ordinarily exactly one member stays (A-22); A-57 widens this to a
 * list, since every Beta-restricted member of a block stays behind when the block breaks.
 */
public record BlockBreak(int cell, List<PieceId> staying, List<PieceId> leaving, int unitsEach) {

    public BlockBreak {
        staying = List.copyOf(staying);
        leaving = List.copyOf(leaving);
    }
}
