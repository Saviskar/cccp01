# LUDO-T — Specification Assumptions

This document records how every ambiguity in the LUDO-T brief is resolved.
It is **binding**: the implementation and tests must follow it exactly.
Each assumption has an ID (`A-xx`) so code comments and tests can reference it.
Items marked ⚠️ are the riskiest interpretations and should be confirmed with the module leader if possible.

---

## 1. Board Geometry

**A-01 — Grid and track numbering.** The board is a 15×15 grid. The standard track has 52 cells, numbered 0–51 clockwise, with index 0 at Yellow's starting square X (grid row 1, col 8).

**A-02 — Colour reference cells** (verified against Figure 1):

| Colour | X (start) | Approach | Home straight cells |
|---|---|---|---|
| Yellow | 0 | 50 | yellowhomepath0 – yellowhomepath4 |
| Blue | 13 | 11 | bluehomepath0 – bluehomepath4 |
| Red | 26 | 24 | redhomepath0 – redhomepath4 |
| Green | 39 | 37 | greenhomepath0 – greenhomepath4 |

The X offset is `13 × colourIndex` (Y=0, B=1, R=2, G=3), and `approach = (X − 2) mod 52`. Both are computed, not hardcoded.

**A-03 — Special cells.** Alpha, Beta and Gamma are counted **clockwise from the Yellow Approach cell (index 50 = 0)**, not from Yellow X:
- Alpha = (50 + 9) mod 52 = **7**
- Beta = (50 + 27) mod 52 = **25**
- Gamma = (50 + 46) mod 52 = **44**

Rationale: the brief's own numbering convention runs clockwise.

**A-04 — Turn order.** Play passes clockwise: **R → G → Y → B → R**, consistent with the brief's example ("if R rolled, next is G").

---

## 2. Movement and Home

**A-05 — Home distance.** From the Approach cell, a piece needs 6 steps to reach Home: homepath0 … homepath4, then Home. A clockwise piece travels 56 steps in total from X.

**A-06 — Rule 9, landing vs. passing.** Landing *on* the Approach cell keeps the piece on the track. Moving *past* the Approach enters the home straight, but only if the piece is eligible (A-07, A-08).

**A-07 — T-7 eligibility.** A piece may enter the home straight only if its capture count is ≥ 1. An ineligible piece continues around the standard track and tries again on its next pass.

**A-08 — T-1, counterclockwise entry.** Each piece has a `ccwApproachCrossings` counter that increments only when the piece moves past its Approach **while moving counterclockwise** without entering the home straight. A counterclockwise piece may enter the home straight once `ccwApproachCrossings ≥ 1`, i.e. from its second crossing onward. Because a counterclockwise piece crosses its Approach 2 steps after leaving X, its full route is 2 + 52 + 6 = 60 steps.

**A-09 — Rule 10, exact roll.** A move that would overshoot Home is illegal for that piece. There is no bounce-back.

**A-10 — Home straight.** Home-straight cells are colour-specific. Pieces there cannot be captured, and same-colour pieces may share a home-straight cell without forming a block.

**A-11 — No safe cells.** The brief defines none, so a piece can be captured on any standard track cell, including X cells and Approach cells.

**A-12 — Direction.** A coin toss (heads = clockwise, tails = counterclockwise) happens only when a piece moves from base to X. Teleports never re-toss.

**A-13 — Distance from home.** "Distance from home" means the number of steps remaining along the piece's route (A-05, A-08), **assuming the piece will be eligible under T-7**, since future captures can't be predicted. A piece at Home has distance 0. A piece in base ranks as farther from home than any piece on the board — its distance is a constant larger than any on-board distance — rather than a computed "full route length + 1", since a Gamma-reversed counterclockwise piece (A-34) can exceed that figure.

---

## 3. Blocks (T-3 to T-6, T-8)

**A-14 — Block definition.** A block is 2 or more same-colour pieces on the same standard track cell. Blocks are **derived from cell occupancy**, never stored separately.

**A-15 — Own-colour pieces.** A player's pieces may pass through or land on that player's own blocks. Only opponents are obstructed.

**A-16 — Obstruction and partial moves.** An opponent single piece cannot land on or pass a block. A partial move to the cell immediately before the block, in the direction of travel, is legal **only if the player has no other legal full move**. If the block is directly adjacent, the partial move covers 0 cells and the piece cannot move.

**A-17 — T-4 block move.** ⚠️ A block may move as a unit by `floor(roll / n)` cells, where n is the block size. The remainder is discarded, and a result of 0 makes the block move illegal. Direction: the members' shared direction if they all agree; otherwise the direction of the member farthest from home (A-13), with a tie going to clockwise.

**A-18 — Block move limits.** Block moves stay on the standard track and never enter a home straight. Pieces must break off individually to go home. A block containing a Beta-restricted piece (A-33) cannot make a block move. Alpha modifiers (A-32) do not apply to block moves.

**A-19 — Block captures a single piece.** A block landing on a single opponent piece captures it, and every member's capture count increases by 1.

**A-20 — T-8, block vs. block.** A block may land on an opponent block **of the same size**, capturing every piece in it; each capturing member's capture count increases by 1. An opponent block of a different size acts as an obstruction (A-16).

**A-21 — T-5, breaking a block.** Every piece permanently stores its original direction, so a piece leaving a block automatically moves in its original direction.

**A-22 — T-6, three sixes with a block.** ⚠️ If a player owns at least one block and rolls three consecutive sixes, the third six is ignored (Rule 4) and **every** block that player owns must break. In each block, the member farthest from home stays (ties go to the lowest piece number). The remaining members share 6 units equally, each moving in its own original direction: 6 for a block of 2, 3 + 3 for a block of 3, 2 + 2 + 2 for a block of 4. Captures made during these moves count, but they grant no bonus roll because the turn has ended.

**A-50 — Block-move obstruction.** A block move (T-4) may pass single pieces of either colour
and the mover's own-colour pieces or blocks (Rule 5), but may not pass an opponent block: if an
opponent block occupies any cell on the block's path, including the landing cell, the block move
is illegal and is not generated. (A same-size opponent block on the landing cell becomes a legal
capture once T-8/A-20 is implemented in phase 4g; until then, and for any different-size opponent
block permanently, it blocks the move.) Blocks never receive a partial move — A-16's partial move
is for single pieces only — so an obstructed block move is silently omitted from the legal move
list, and A-48's blocked messages do not apply to it. Landing on a single opponent piece captures
it (A-19). Landing on an own-colour cell merges into a (larger) block.

**A-51 — Block capture message.** When a block captures a single opponent piece (A-19), one
capture fact is published, naming the block's lowest-numbered member as the capturer (consistent
with A-40 and A-48); every member's capture count still increments. The block's movement fact is
published first, then the capture fact (A-46).

**A-52 — Obstruction during a forced break (T-6).** If a member leaving a block under A-22 is
obstructed by an opponent block, it moves as far as it can (a partial move to the cell before
the block, A-16), publishing the same "is blocked" and partial-move facts as an ordinary
obstructed move. If the block is adjacent, the member stays put and only the "is blocked" fact
is published — never the "ignoring the throw" fact, since a forced break is not a throw.
Captures during these moves count but grant no bonus roll (A-22).

**A-48 — Blocked messages.** Obstruction is only reported when it decides the turn (A-16). If a partial move is made, only that piece publishes the "is blocked from moving from L1 to L2" fact followed by the partial-move fact. If no legal move exists at all and at least one piece is obstructed, each obstructed piece publishes the "is blocked" fact, followed by a single "ignoring the throw" fact for the player — and no `NoLegalMove`. If no legal move exists and nothing is obstructed, only `NoLegalMove` is published. When any full move exists, obstruction is silent. L2 = where the full roll would have taken the piece; the blocking piece named is the lowest-numbered piece in the block.

**A-49 — Overshoot beats obstruction.** If the full roll would overshoot Home (A-09), the move is illegal for that piece even if an opponent block would have stopped it earlier; no partial move is offered and no blocked message is published. Only reachable once Alpha's energised effect doubles a roll (A-32).

**A-53 — T-8 multi-piece capture.** When a block captures a same-size opponent block (T-8/A-20),
one capture fact (plus its count line) is published per captured piece, in ascending
piece-number order, each naming the capturing block's lowest-numbered member as capturer
(A-51) and reusing the brief's capture template verbatim (A-43). Every capturing member's
capture count increases by exactly 1 for the whole T-8 capture, not once per captured piece,
and the capture grants a single bonus roll (A-23).

---

## 4. Captures and Turns

**A-23 — Bonus rolls.** A single roll grants at most one extra roll, whether from rolling a six or from a capture (T-2). The two do not stack.

**A-24 — Six streak.** The consecutive-six counter resets on any roll that is not a six. A bonus roll earned through a capture counts as a normal roll in the streak.

**A-25 — Entering from base.** Moving from base to X follows normal landing rules: a single opponent piece on X is captured (with the T-2 bonus roll); an opponent block on X makes the move illegal; an own piece on X forms a block.

**A-26 — T-9, reset.** Any return to base, whether by capture or by a Base teleport, resets all of the piece's state: capture count, direction, crossing counter and active effects.

**A-27 — Opening roll ties.** If players tie for the highest opening roll, only the tied players re-roll, and every re-roll is printed.

---

## 5. Mystery Cell and Effects

**A-28 — T-10 timing.** ⚠️ The spawn timer starts at the end of the first round in which any piece is on the standard track. The mystery cell spawns at the end of the 2nd full round after that. It stays for 4 rounds, then immediately moves to a new random cell. Valid cells are standard-track cells that are empty at spawn time and are not the previous location. The `<N>` in the round-end message is the number of rounds remaining (4, 3, 2, 1).

**A-29 — Triggering.** Only an individual piece that **ends** its move on the mystery cell triggers it. Passing over it, block moves, entering from base, and arriving by teleport do not trigger it (teleports never chain).

**A-30 — Teleport outcomes (T-11).** Each of the 6 outcomes has equal probability. The piece keeps its direction and counters, except for a Base teleport, which resets it (A-26).

**A-31 — Teleport onto an occupied cell.** If a single opponent piece is at the destination, it is captured (the T-2 bonus roll and capture count apply). If an opponent block is there, the teleported piece is sent to base. If an own piece is there, a block forms.

**A-32 — Alpha (T-12).** Energised or sick, with 50/50 odds. The effect lasts until the end of the 4th full round after the teleport. The effective movement is `roll × 2` (energised) or `floor(roll / 2)` (sick), consistent with the integer division in A-17. An effective value of 0 means the piece cannot move with that roll. The **raw** roll still decides sixes, bonus rolls and base exits.

**A-33 — Beta (T-13).** ⚠️ The piece cannot move until the end of the 4th full round after the teleport. "Rolls three consecutively" means the owning player rolls the **value 3** three times in a row; the streak may span turns, and any other value resets it. On the third 3, the piece is teleported to base (A-26). A restricted piece can still be captured.

**A-34 — Gamma (T-14).** A clockwise piece permanently becomes counterclockwise, and this becomes its new original direction for T-5. A counterclockwise piece is teleported to Beta, and the Beta effect (A-33) applies because the chain started at a mystery cell.

**A-35 — T-15.** Landing on Alpha, Beta or Gamma by a normal move has no effect.

**A-45 — One effect at a time.** A piece has at most one active effect. A new Alpha or Beta effect replaces any existing one, and its duration restarts.

**A-54 — Forced breaks and the mystery cell.** A member moved by a T-6 forced break (A-22) that ends its move on the mystery cell triggers it (A-29). Any capture from the resulting teleport counts but grants no bonus roll, since the turn has ended (A-22).

**A-55 — Effects apply only to rolled moves.** Alpha's energised/sick adjustment (A-32) applies only to a piece's individual move made from a roll. It does not apply to T-6 forced break moves (A-22), which always move their fixed share — keeping forced moves within 6 units — nor to block moves (A-18).

**A-46 — Move-then-capture message order.** A capturing move publishes the movement message first, then the capture message — never the capture message alone. A standard-path capture publishes the "moves piece from L1 to L2" message, then the capture message. A base-to-X capture publishes the "moved to the starting point" message, then the capture message. The player-count line that follows a capture message reports the captured colour's board/base counts, since the capturing player's own counts don't change.

**A-47 — Six always grants a bonus roll.** Rule 4's second roll for a six is unconditional: a player who rolls a six gets a bonus roll even if that six produced no legal move (e.g. no piece could leave base, or the only piece on the board couldn't move). Exception: when the throw is ignored because of an obstruction (A-48), the turn ends and the dice passes to the next player, even on a six — as the spec's "Ignoring the throw and moving on to the next player" message and Rule 7 state.

---

## 6. Player Behaviour Clarifications

**A-36 — Red.** "Closest to its home" means the opponent piece closest to **that opponent's own** home, since resetting it does the most damage. On a six: capture if possible, otherwise bring a piece out of base (if any are there). On any other roll: capture if possible; otherwise move the piece closest to home whose move does not form a block, and form a block only if every legal move does.

**A-37 — Green.** When none of its stated rules decide the move, Green moves the non-block piece closest to home.

**A-38 — Yellow.** Follows the brief as written. "Pieces that need captures" means pieces with capture count 0.

**A-39 — Blue.** The cycle pointer advances on every move, including bonus rolls, and skips pieces that cannot move (at Home, restricted, or in base without a six). The candidate moves for the scheduled piece are its individual move, any block move it belongs to, and base → X on a six.
- A counterclockwise piece picks a candidate that lands on the mystery cell if one exists.
- A clockwise piece avoids candidates that land on the mystery cell. If its only legal move lands there, Blue tries the next piece in the cycle, and takes the mystery landing only if every piece is forced into it.
- When the preference doesn't decide, Blue picks randomly among the candidates.

**A-40 — Strategy tie-breaks.** Any remaining tie between moves is broken by lowest piece number, keeping seeded runs reproducible.

---

## 7. Game End

**A-41 — Placings.** Players who have finished are skipped. The game continues until three players have finished; the fourth place is assigned automatically.

**A-42 — Round guard.** The game ends after **1,000 rounds** if not already complete, and the current standings are printed. This is necessary because stalemates are possible: under T-7, a piece may be unable to go home once no opponent pieces remain to capture.

---

## 8. Output

**A-43 — Message text.** All messages are reproduced **verbatim** from Section 3 of the brief, including its wording quirks (e.g. "[Number]/4 on pieces on the board"). Colour names are capitalised when they begin a sentence.

**A-44 — Messages not in the brief.** Events with no specified message (e.g. an illegal overshoot, a coin toss result, a block move, T-6 breaks, the round guard) use clearly worded additional messages that follow the same style.

---

## 9. Phase 4j Interactions (Beta Briefing, T-13)

**A-56 — T-13 streak scope and multi-piece release.** The three-consecutive-3s streak (A-33)
is tracked per colour, counting only rolls made while at least one of that colour's pieces is
Beta-restricted; it resets to 0 whenever the colour has no restricted piece, and also resets to
0 immediately after triggering. When the streak reaches three, every one of that colour's
currently Beta-restricted pieces is teleported to base (A-26), not just one.

**A-57 — T-6 interaction with a Beta-restricted block member.** If a block that must break under
T-6 (A-22) contains one or more Beta-restricted members (A-33), those members are the ones that
stay; every unrestricted member leaves, sharing the 6 units equally in its original direction (6
for 1 leaver, 3 each for 2 leavers, 2 each for 3 leavers). If no member is restricted, A-22
applies unchanged. If every member is restricted, the block is omitted from the break plan
entirely — nothing breaks, and no `BlockadeBroken` is published for it.

**A-58 — Gamma details.** A counterclockwise piece teleported to Gamma (A-34) first lands on
Gamma under A-31 — a single opponent there is captured; an opponent block sends it to base and
the chain ends — then is teleported on to Beta, again under A-31, where Briefing (A-33) applies
if it lands. Captures at both cells count, but grant at most one bonus roll (A-23). When Gamma
turns a clockwise piece counterclockwise, its counterclockwise crossing count is unchanged (it
counts only counterclockwise passes, A-08), so it must pass its Approach counterclockwise twice
before entering the home straight.

---

## 10. Phase 5 Output Layer

**A-59 — Piece references in move messages.** In Section 3's templates, "X" is the colour
placeholder, so "piece X[Name]" renders as the colour letter plus piece number (e.g. R1),
matching §1.1's naming. The ordinary move template's "piece X" is read the same way and also
renders as R1-style (e.g. "Red moves piece R1 from location 5 to 11 by 6 units in clockwise
direction."), since a bare "X" would not identify the piece. Templates written as "[Color X]
piece [Name]" keep the bare number (e.g. "Red piece 1").

**A-60 — Redirected teleport messages.** When A-31 redirects a teleport to base, the "lands on a
mystery cell and is teleported to \<location\>" line names the drawn destination; then an extra
A-44 line explains the redirect (e.g. "Alpha is occupied by a green blockade, so red piece 1 is
sent to base instead." — A-43's capitalise-only-at-sentence-start rule applies: the special-cell
name opens the sentence and is capitalised, the two colour words are mid-sentence and stay
lowercase); then the per-piece line uses the Base template ("teleported to Base."), reflecting
where the piece actually ends up.

**A-61 — Round-guard ending.** When the round guard (A-42) ends the game, no placing is
re-announced — every finisher already got its live message (A-41, including place 1's
"wins!!!"). Instead one summary line is printed: "The game reached the 1000-round limit.
Finished: 1st red, 2nd green. Did not finish: yellow, blue." Finishers are listed in finishing
order (reusing `GameEnded.placings()`, which `Standings.finalPlacings()` already returns
verbatim under the guard); non-finishers are listed unranked, in the fixed turn order R-G-Y-B
(A-04). If nobody finished, the "Finished" part reads "none". Both lists are plain
comma-separated (no "and" before the last item, unlike `roundOrderAnnounced`'s list).

---

## 11. Phase 6a — Red Strategy

**A-62 — Red ranking for multi-piece captures.** When a capturing move would capture more than
one piece (T-8), Red ranks it by the captured piece closest to its own home (A-36). The number
of pieces captured does not affect the ranking.

**A-63 — Red and block moves.** For A-36, a block move counts as forming a block (its pieces
remain a block), so Red moves a block as a unit only when every legal move forms a block.

**A-64 — Red, six with no capture or base-exit.** If Red rolls a six and neither a capture
nor a legal base-exit is available (e.g. no piece remains in base), Red falls back to
A-36's non-six rule: move the piece closest to home whose move does not form a block,
forming a block only if every legal move does.

---

## 12. Phase 6b — Green Strategy

**A-65 — Green's priority order.**
1. On a six with a piece in base: if a single piece's move would create a new block
   (landing on another Green piece — a block move does not count), take it; otherwise bring
   a piece out of base (lowest number).
2. Otherwise, a block move if legal.
3. Otherwise, a move that does not break a block, closest to home (A-37).
4. Only if every legal move breaks a block, a breaking move, closest to home.

Remaining ties go to the lowest piece number (A-40).

**A-66 — Green: "in front of the block".** Read together with bullet 2 ("prioritises moving
its other pieces home before breaking a block"), Green breaks a block only when every legal
move breaks a block; any legal non-breaking move, by any piece, is preferred (closest to
home, A-37). The brief's "pieces in front of it" is treated as the common case of this rule,
not a separate positional test.

**A-67 — Green and captures.** The brief's "will not look to capture opponent pieces more
than what is required to enter the home straight" is a limit, not a mandate: Green has no
capture-priority tier. A capturing move is ranked like any other move within A-65's tiers.

---

## 13. Phase 6c — Yellow Strategy

**A-68 — Yellow and block-move captures.** For a capturing block move, Yellow's capture
tier (A-38) treats the move as eligible if **any** member has a capture count of 0 —
since a block capture credits every member (A-19), it helps any member that still needs
one. Other move types check their single mover.

**A-69 — Yellow's capture tie-break.** Yellow's capture tier (A-38) has no distance-based
ranking among multiple qualifying captures (unlike Red/A-62). Ties break by the mover's
lowest piece number (A-40) only.

**A-70 — Yellow and blocks.** The brief gives Yellow no block preference, so Yellow
neither avoids nor seeks forming or breaking blocks. In the closest-to-home tier, a block
move is ranked like any other move, by its lowest-numbered member (`MoveRanking.mover`);
"by the number specified in the roll" is read as describing the usual case, not as
excluding block moves.

---

## 14. Phase 6d — Blue Strategy

**A-71 — Blue when every piece is forced onto the mystery cell.** If, sweeping from the
cycle pointer, every movable piece is clockwise and every one of its candidate moves lands
on the mystery cell (A-39), Blue takes the first such piece in the sweep — choosing among
its candidates via the `RandomPicker` — and the pointer moves to the piece after it.

---

## 15. Phase 7 — Composition Root

**A-72 — Seed announcement wording and placement.** `SeedSelected` is not
a brief event; A-44 licenses an additional message, but the exact wording
and the decision to publish it first (ahead of every other event,
including `PiecesIntroduced`) are recorded here, not left to a generic
A-44 reference. The message follows the brief's prose style as two
sentences (cf. A-60's redirect line): "This run uses seed <n>." then
"Rerun with --seed <n> to reproduce this exact game." Publishing it first
means a run's seed is known even if the transcript is interrupted or
truncated.
