package ludot.events;

/** Observer of published game events (Observer, DESIGN.md 4.3). */
public interface GameEventListener {
    void onEvent(GameEvent event);
}
