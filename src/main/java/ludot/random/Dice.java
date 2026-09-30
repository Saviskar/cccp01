package ludot.random;

/** Injected source of dice rolls (F4) — only SeededDice may use java.util.Random. */
public interface Dice {
    int roll();
}
