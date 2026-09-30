package ludot.events;

/** A fact the engine publishes for ConsoleReporter to format (Observer, DESIGN.md 4.3). */
public sealed interface GameEvent permits
        PiecesIntroduced,
        OpeningRollRolled,
        OpeningRollWinnerDetermined,
        RoundOrderAnnounced,
        DiceRolled,
        PieceEnteredX,
        PieceMoved,
        PieceCaptured,
        PieceCountStatus,
        NoLegalMove,
        ThirdSixIgnored,
        RoundStatusReported,
        PlayerFinished,
        GameEnded {
}
