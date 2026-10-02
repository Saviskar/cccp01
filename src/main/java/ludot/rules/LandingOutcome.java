package ludot.rules;

/**
 * Result of {@link MysteryLanding#landAt}: whether it captured an opponent piece, and whether an
 * opponent block redirected the teleport to base instead of landing there (A-31).
 */
record LandingOutcome(boolean captured, boolean redirectedToBase) {
}
