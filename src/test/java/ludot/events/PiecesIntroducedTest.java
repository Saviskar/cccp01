package ludot.events;

import ludot.domain.Colour;
import ludot.domain.PieceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PiecesIntroducedTest {

    @Test
    @DisplayName("accepts exactly 4 piece ids")
    void acceptsFourPieceIds() {
        List<PieceId> ids = List.of(
                new PieceId(Colour.RED, 1),
                new PieceId(Colour.RED, 2),
                new PieceId(Colour.RED, 3),
                new PieceId(Colour.RED, 4));

        PiecesIntroduced event = new PiecesIntroduced(Colour.RED, ids);

        assertEquals(Colour.RED, event.colour());
        assertEquals(ids, event.pieceIds());
    }

    @Test
    @DisplayName("rejects a piece id list that isn't size 4")
    void rejectsWrongSizedList() {
        List<PieceId> tooFew = List.of(new PieceId(Colour.RED, 1));
        assertThrows(IllegalArgumentException.class, () -> new PiecesIntroduced(Colour.RED, tooFew));
    }
}
