package ludot.engine;

import ludot.domain.Colour;

import java.util.ArrayList;
import java.util.List;

/** Tracks finish order for Rule 11/A-41: the game ends once 3 players have finished. */
public final class Standings {

    private static final int PLAYERS_NEEDED_TO_END = 3;

    private final List<Colour> finishOrder = new ArrayList<>();

    public void recordFinish(Colour colour) {
        if (finishOrder.contains(colour)) {
            throw new IllegalStateException(colour + " has already finished");
        }
        finishOrder.add(colour);
    }

    public boolean hasFinished(Colour colour) {
        return finishOrder.contains(colour);
    }

    public boolean isOver() {
        return finishOrder.size() >= PLAYERS_NEEDED_TO_END;
    }

    public List<Colour> finishOrder() {
        return List.copyOf(finishOrder);
    }

    /**
     * A-41: once exactly 3 colours have finished, the remaining one is placed
     * fourth automatically. Under the A-42 round guard, fewer than 3 may have
     * finished; the partial order is returned as-is, since there's no rule to
     * rank the colours that never reached Home.
     */
    public List<Colour> finalPlacings(List<Colour> allColours) {
        if (finishOrder.size() != PLAYERS_NEEDED_TO_END) {
            return finishOrder();
        }
        List<Colour> placings = new ArrayList<>(finishOrder);
        for (Colour colour : allColours) {
            if (!placings.contains(colour)) {
                placings.add(colour);
                break;
            }
        }
        return List.copyOf(placings);
    }
}
