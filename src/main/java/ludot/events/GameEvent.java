package ludot.events;

/** A fact the engine publishes for ConsoleReporter to format (Observer, DESIGN.md 4.3). */
public sealed interface GameEvent permits
        PiecesIntroduced,
        OpeningRollRolled,
        OpeningRollWinnerDetermined,
        RoundOrderAnnounced,
        DiceRolled,
        PieceEnteredX,
        PieceDirectionAssigned,
        PieceMoved,
        BlockMoved,
        PieceCaptured,
        PieceCountStatus,
        PieceBlocked,
        PiecePartiallyMoved,
        ThrowIgnoredAfterBlock,
        NoLegalMove,
        ThirdSixIgnored,
        BlockadeBroken,
        RoundStatusReported,
        PlayerFinished,
        GameEnded,
        MysterySpawned,
        MysteryCellStatusReported,
        MysteryCellTriggered,
        PieceTeleported,
        AlphaEffectAssigned,
        BriefingAssigned,
        BriefingStreakTriggered {
}
