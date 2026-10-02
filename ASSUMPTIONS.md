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

**A-13 — Distance from home.** "Distance from home" means the number of steps remaining along the piece's route (A-05, A-08), **assuming the piece will be eligible under T-7**, since future captures can't be predicted. A piece at Home has distance 0. A piece in base has distance = full route length + 1.

---

## 3. Blocks (T-3 to T-6, T-8)

**A-14 — Block definition.** A block is 2 or more same-colour pieces on the same standard track cell. Blocks are **derived from cell occupancy**, never stored separately.

**A-15 — Own-colour pieces.** A player's pieces may pass through or land on that player's own blocks. Only opponents are obstructed.

**A-16 — Obstruction and partial moves.** An opponent single piece cannot land on or pass a block. A partial move to the cell immediately before the block, in the direction of travel, is legal **only if the player has no other legal full move**. If the block is directly adjacent, the partial move covers 0 cells and the piece cannot move.

**A-17 — T-4 block move.** ⚠️ A block may move as a unit by `floor(roll / n)` cells, where n is the block size. The remainder is discarded, and a result of 0 makes the block move illegal. Direction: the members' shared direction if they all agree; otherwise the direction of the member farthest from home (A-13), with a tie going to clockwise.

**A-18 — Block move limits.** Block moves stay on the standard track and never enter a home straight. Pieces must break off individually to go home. A block containing a Beta-restricted piece (A-31) cannot make a block move. Alpha modifiers (A-30) do not apply to block moves.

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
