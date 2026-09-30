package ludot.random;

import ludot.domain.Direction;

/** Injected coin toss deciding a piece's direction after leaving base (A-12). */
public interface Coin {
    Direction toss();
}
