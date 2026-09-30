package ludot.domain;

/**
 * Where a piece is. The four variants are mutually exclusive (a piece is never
 * "on track and in base" etc.), so invalid combinations can't be represented.
 */
public sealed interface Position permits InBase, OnTrack, InHomeStraight, AtHome {
}
