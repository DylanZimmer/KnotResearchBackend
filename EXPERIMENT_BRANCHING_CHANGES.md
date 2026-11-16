# Experiment branching (substates) and `runExperimentWithGoal`: change log

This file lists every change made to finish `runExperimentWithGoal` and add substates for branching, with the reason for each one.

- **Round 1:** substates, branching, and the invariants/JPA fixes.
- **Round 2 (2026-11-10):** your answers to my questions:
  - repeat the move list until the goal is hit (up to 1000 passes)
  - `numTimes` means n separate applications
  - slides come from `slideableList`
  - `switchPlacement` fix

  Two more bugs turned up along the way (in `Slide` and `addTwist`); see "Round 2 changes".

---

## How it works now

A state is identified by `(experimentId, ogKnotId, stateNum, subStateNum)`:

| field | meaning |
|---|---|
| `stateNum` | depth: how many moves have been applied since the start state |
| `subStateNum` | which branch at that depth (0, 1, 2, ... numbered per knot per depth) |
| `parentSubStateNum` (new column) | the substate at `stateNum - 1` this state was made from (null for the start state) |
| `moveDetail` (new column) | where the move was applied, e.g. `strand=3, sign=pos` or `triangle=[2, 5, 7]` |

The start state is `(exp, knot, 0, 0)`. To trace a path back from any state, follow `parentSubStateNum` down one `stateNum` at a time.

### `runExperimentWithGoal(experimentId, moveRequests, goal, branch)`

**The goal (`hitGoal`).** The controller takes a `goal` holding one or more invariants from `InvariantsDto`, chosen freely: `Alexander_Polynomial`, `Determinant`, `Writhe`, or any combination. A state hits the goal when every chosen invariant equals its value; invariants not chosen are ignored. For example:
- `{"values": {"Determinant": 1}}` matches on determinant only.
- `{"values": {"Determinant": 1, "Alexander_Polynomial": "1"}}` needs both to match.

**The loop.** For each knot:
```
while (!hitGoal && iterations < 1000) {     // MAX_GOAL_ITERATIONS
    iterations++
    for each move request in the list:
        apply it numTimes times, one at a time (each one a new level / state)
        if any state hit the goal -> stop
}
```
- **`numTimes = n`:** the move is applied n times before the next move in the list. Each application is its own saved state, and **where it's applied is recalculated from the current diagram every time**.
  - `null` means once.
  - `numTimes = 0` keeps its existing meaning ("every fitting location at once", as one state) without branching. With branching it means one level.
- **`branch = false`:** one path on `subStateNum = 0`. Each application uses the first location that fits.
  - If a whole pass through the list changes nothing (no move fits anywhere), it stops right away instead of repeating that 1000 times.
- **`branch = true`:** breadth-first tree. At each application, every state in the current level gets one child substate per location the move fits, found with `LocationSpecifyingService.convertRawRequestArgsAll` on that state's diagram.
  - The search stops after the first level where any substate hits the goal. That whole level is finished first, so every way of reaching the goal in the minimum number of moves is recorded.
- **Limits:**
  - 1000 passes through the list (`MAX_GOAL_ITERATIONS`)
  - with branching, also 5000 states in one level per knot (`MAX_STATES_PER_LEVEL`). Branching grows exponentially and each state costs a SageMath call. The next level is built in memory and checked against this limit before any Sage calls are made.
- **Return value:** the list of states that reached the goal. Their `experimentStatus` is `"Goal Reached"`.
- **Experiment status** is set to one of:
  - `"Goal Reached"` (every knot)
  - `"Goal Reached For Some Knots"`
  - `"Stopped: Branch Limit Reached"`
  - `"Stopped: 1000 Iterations Without Reaching Goal"`
  - `"Goal Not Reached"` (no move could change the diagram any further)
- **Dead ends:** a state where the next move fits nowhere gets status `"No Moves Available"`.
- **Re-run guard:** the method refuses (409) if the experiment already has states past state 0, because re-running would overwrite rows that are already there.

### Slides in experiments
- The possible slides are the triangles from `FullNotationMovesService.slideableList`, using the experiment diagram's boundaries.
- Degenerate triangles are dropped (see `isSlideableTriangle` below), as are duplicates.
- `SlideLoc` filters are optional:
  - `crossing`: at least one of the triangle's crossings matches the given sign / placement
  - `strand`: at least one of its strands matches the strand type
- Each slide branch's `moveDetail` is `triangle=[a, b, c]`.

### Endpoint

```
POST /api/exp/run_experiment_with_goal?experimentId=12
{
  "moveRequests": [
    {"type": "addTwist", "location": null, "typeSpecificChoice": "pos", "numTimes": 2},
    {"type": "slide", "location": {"crossing": {"sign": "neg"}}, "numTimes": 1}
  ],
  "goal": {"values": {"Determinant": 1}},
  "branch": true
}
```
It returns the goal states. The full tree is still available from `GET /api/exp/experiment_states`.

---

## Round 10 changes (2026-11-14): experiment ids start at 0 and go up by 1

- **Why ids jumped:** `Experiments.experimentId` used `@GeneratedValue(strategy = SEQUENCE)` with Hibernate's default block size of 50. Each time the backend starts it reserves a block of 50 ids, so ids skipped (the test experiment after the migration was 1002).
- **Now** `@SequenceGenerator(sequenceName = "experiments_seq", initialValue = 0, allocationSize = 1)`: one id per experiment, 0, 1, 2, ...
- **The database sequence has to match before the new backend starts.** Hibernate 6 refuses to start when `experiments_seq`'s increment (50 today) differs from `allocationSize` (1). Run the SQL below, then rebuild the backend straight away. Don't start experiments in between: the old build would hand out overlapping ids.

  ```sql
  -- Check the sequence first (expect increment_by 50):
  SELECT sequencename, min_value, increment_by, last_value FROM pg_sequences WHERE sequencename = 'experiments_seq';

  BEGIN;
  TRUNCATE experiments_full_notation, experiment_states, experiments;
  ALTER SEQUENCE experiments_seq INCREMENT BY 1 MINVALUE 0 RESTART WITH 0;
  COMMIT;
  ```
- **Id 0 works everywhere:** the frontend's id checks only reject negative numbers, and nothing treats 0 as "no experiment".
- **New test** `BackendApplicationTests.experimentIdsStartAtZeroAndGoUpByOne` (H2). Backend total: 58 run, 57 pass (note 3's old failure).

---

## Round 9 changes (2026-11-14): strand filters point the right way

- **`StrandTypeDto.crossing_before` / `crossing_after` were reversed.** `LocationSpecifyingService.checkStrand` matched `crossing_before` against strands running *into* a crossing, but the frontend (and the name) mean the crossing a strand *starts* at.
  - Now `crossing_before` is the crossing the strand starts at and `crossing_after` the one it ends at.
  - `placement` says whether the strand is over or under there.
  - This affects `addTwist` locations, the `strands` filters in poke and saddle regions, and the `strand` filter for slides.
- **Cleaner code:** the four copied blocks (sign/placement × before/after) are now one helper, `strandsAtCrossings`. It still handles `orientedWith = false` by swapping the ends, ready for when strands can be oriented against the diagram (today `orientAllStrands` marks every strand `true`).
- **New `StrandFilterTest`** (4 tests) pins the direction on the figure-eight: the start and end of each strand, by sign and placement, both ends together, empty filters, and an `addTwist` location.
- **Experiments run before this fix** with a strand filter used the opposite end of each strand.
- **The AI service prompt** now describes the fixed behavior.
- **Backend total:** 57 run, 56 pass (only note 3's old failure is left).

---

## Round 8 changes (2026-11-14): the AI service is hooked up to KnotResearch

The AI service takes a problem written in ordinary language and proposes experiments in the exact JSON the backend runs. It doesn't run them. Details are in `../AI_Service/README.md`.

### What was broken
- **The container crashed on startup.** `main.py` used `ResearchQuestion`, `HTTPException` and `create_experiment_plan` without defining or importing them.
- **`huggingface_hub` wasn't in `requirements.txt`.**
- **`model.py` raised an error on import when `HF_TOKEN` was missing,** so even `/health` died. The client is now created on first use, and a missing token gives a 503.
- **The model's reply was parsed raw.** Qwen3 Thinking puts its reasoning (`<think>…</think>`) before the answer, so parsing always failed. `max_tokens=2000` was also usually used up by the thinking. Now the JSON is taken out of the reply, `HF_MAX_TOKENS` defaults to 16000, and an invalid reply is sent back to the model with the errors (up to 2 more times).
- **The token would have been copied into the image** (`COPY . .` with no `.dockerignore` entry for `.env`). `.env` is now ignored by Docker and git, and passed in at run time with `env_file`. I checked the built image: no `.env`.
- **The plan wasn't tied to KnotResearch.** It returned free text about "operations to consider", so nothing could be run.

### What it does now
- **The output is checked against the backend's real request formats** (`experiment_schema.py`, mirroring `MoveRequest` and its subtypes, the location DTOs, and `GoalExperimentRequest`), with unknown fields rejected.
- **Each experiment comes with the calls that run it.**
- **Knot names are checked against the live database.** Each one must exist and have a stored diagram: `startExperiment` on a knot with no FullNotation would quietly run on an empty diagram (the unknot).
- **The prompt was rewritten from the code**, covering:
  - the knot catalog (sent with every request)
  - FullNotation
  - every move: what it changes, and which moves keep the knot type
  - the exact JSON for each move, and how the location filters really behave
  - `numTimes`
  - runs without a goal, with a goal, and branching, plus the 5000-state and 1000-pass limits
  - the statuses
  - the three invariants and SageMath's polynomial format, and what isn't computed (Jones etc., knot identification)
  - research patterns: unknotting, crossing-change distance, band moves, families, chirality, simplification
  - rules on evidence versus proof

### Things I found in the backend (not changed)
- **(Fixed in Round 9.) `StrandTypeDto.crossing_before` / `crossing_after` look reversed.** `checkStrand` matches `crossing_before` against strands running *into* a crossing (the crossing at the strand's end). The frontend sends a strand's *first* crossing as `crossing_before`. The prompt describes the behavior as it is.
- **`orientedWith` filters nothing:** `orientAllStrands` marks every strand `true`. The prompt says to leave it null.

### Checked
- 7 new tests, run in the image, with a fake model and a fake backend.
- In the running container: `/health` is fine. It read the knot list from the backend container and found `0_1`, `3_1`, `4_1`, `10_165`, `11n_34` and `13n_5110`, each with a stored diagram.
- **Not checked: a real model answer.** Hugging Face refused the call with "402 Payment Required" because the account has no Inference Provider credits. Everything up to the model call works.

---

## Round 7 changes (2026-11-14): the AI service runs with the app

`../AI_Service` (FastAPI, only `GET /health` so far) now runs as its own container. No AI features were added, and the backend doesn't call it yet.

- **`AI_Service/requirements.txt` (new):** its Dockerfile installs from it, but it didn't exist, so the image couldn't build. It pins the same FastAPI and uvicorn versions as the SageMath service.
- **`AI_Service/.dockerignore` (new):** keeps caches and virtual environments out of the image.
- **`docker-compose.yml`:** new `ai-service`, built from `../AI_Service`, with a health check on `/health`.
  - It listens on 8000 inside the container, like SageMath, but on **8001** on your machine (SageMath already has 8000). Set `AI_SERVICE_PORT` to change it.
  - Other containers can reach it at `http://ai-service:8000`.
- **`dev.ps1`** starts and stops it along with SageMath.
- **Checked:** built and started next to your running backend and SageMath containers (left untouched); it reports healthy and `http://localhost:8001/health` returns `{"status":"running"}`.

---

## Round 6 changes (2026-11-13): move types saved by name, and the Supabase migration

### `ExperimentStates.moveType` is now `@Enumerated(EnumType.STRING)`
- **Why:** Supabase's `experiment_states.move_type` is `varchar`, and you want it to hold names (`addTwist`), but with no `@Enumerated` Hibernate saved the enum's position (`3`). Existing rows hold numbers.
- **Effects:**
  - New rows are saved as names, matching the column.
  - `MoveType` values can now go in any order; reordering no longer changes the meaning of saved rows.
  - Fixes the old `TINYINT` test failure (note 2). Backend: 52 run, 51 pass; the one left is note 3.
- **Existing rows must be converted** (SQL below), or reading them will fail once this is deployed. The enum's order has not changed since it was first committed (2026-10-11), so the numbers map as: 0 startExperiment, 1 mirror, 2 flip, 3 addTwist, 4 addPoke, 5 slide, 6 saddle (7 to 10, the removals and switchPlacement, were never deployed).

### Supabase migration (your real constraint names)
The `experiment_states` primary key is named `experiment_invariants_pkey`.

```sql
BEGIN;

ALTER TABLE experiment_states ADD COLUMN sub_state_num bigint NOT NULL DEFAULT 0;
ALTER TABLE experiment_states DROP CONSTRAINT experiment_invariants_pkey;
ALTER TABLE experiment_states
  ADD CONSTRAINT experiment_states_pkey PRIMARY KEY (experiment_id, og_knot_id, state_num, sub_state_num);

ALTER TABLE experiments_full_notation ADD COLUMN sub_state_num bigint NOT NULL DEFAULT 0;
ALTER TABLE experiments_full_notation DROP CONSTRAINT experiments_full_notation_pkey;
ALTER TABLE experiments_full_notation
  ADD CONSTRAINT experiments_full_notation_pkey
  PRIMARY KEY (experiment_id, og_knot_id, state_num, sub_state_num, placement, crossing_id);

UPDATE experiment_states
   SET invariants = json_build_object('Alexander_Polynomial', alexander_polynomial,
                                      'Determinant', determinant, 'Writhe', writhe)::text
 WHERE invariants IS NULL;

UPDATE experiment_states
   SET move_type = (ARRAY['startExperiment','mirror','flip','addTwist','addPoke','slide','saddle',
                          'remTwist','remPoke','remPokesAndTwists','switchPlacement'])[move_type::int + 1]
 WHERE move_type ~ '^[0-9]+$';

COMMIT;
```

---

## Round 5 changes (2026-11-13): Switch Crossing Placement as an experiment move

Experiments can now use the crossing change (`switchPlacement` in `FullNotationMovesService`). With branching and a goal such as determinant 1 or Alexander polynomial 1, this is the un-knotting experiment: every way to reach the goal with the fewest crossing changes.

- **`MoveType.switchPlacement`:** new value (added last; since Round 6 the order no longer matters).
- **New `SwitchPlacementRequest(CrossingTypeDto location, Long numTimes)`,** registered in `MoveRequest` as `"switchPlacement"`. The location's `sign` optionally limits which crossings; `placement` is ignored, since every crossing has an over and an under passage.
- **`LocationSpecifyingService`:** every crossing (or every crossing of the chosen sign) fits, in id order. `identifyAllCids` returns nothing when given no filter, so "any crossing" lists the crossings directly.
- **`ExperimentsService.applyMove`** calls `switchPlacement(fnList, cid)`. Branches record `cid=N`.
- **Repeats:**
  - Without branching, each repeat switches the first fitting crossing. Without a sign filter that's the same crossing every time, so 2 repeats undo each other. With a sign filter, the switched crossing no longer matches, so the next repeat moves on.
  - Everywhere at once (`numTimes = 0`) switches every fitting crossing; crossing ids don't change, so they're all found up front.
- **The Visual Component already had this move** (`POST /api/moves/switch_placement`); only experiments were missing it.
- **Tests:**
  - New `SwitchPlacementExperimentTest`: all crossings fit without a filter, none with the wrong sign; branching on the trefoil with goal determinant 1 finds exactly the 3 one-crossing unknottings; switching every crossing gives the mirror.
  - `MoveRequestJsonTest` covers the new JSON.
  - Backend total: 52 run, 50 pass (the same 2 `BackendApplicationTests` failures).
- ~~**Possible database issue:**~~ Doesn't apply: `move_type` on Supabase is `varchar`, not a number column, so it has no range check. See Round 6. Original note: Hibernate 6 adds a check constraint like `check (move_type between 0 and 9)` when it creates the table, and `ddl-auto=update` never widens it. If Supabase's `experiment_states` has one, saving `switchPlacement` (10), and possibly the Round 4 removals, would fail. Look with `\d experiment_states`; if it's there, drop it or widen it to `between 0 and 10`. I didn't touch the database.

---

## Round 4 changes (2026-11-12): removing twists and pokes

You added `remTwist`, `remPoke` and `remPokesAndTwists` to `MoveType`. They're now hooked up in experiments, in `/api/moves`, and in the frontend.

### `services/FullNotationMovesService.java`: removal functions rewritten
Your originals are saved in `REMOVALS_ORIGINAL.java.txt`. They weren't called anywhere, and none of them would have worked:
- **`remTwist`:** on the strand going into the twist, it set `cidBefore` / `strandBefore` where it needed `cidAfter` / `strandAfter`. It also edited the input rows in place.
- **`remPoke`:** both branches wrote to `cid1Under` / `cid1Over`, so `cid2Over` and `cid2Under` stayed null, and the fourth `if` repeated `cid1` where it meant `cid2`. Strands were never re-linked.
- **`getAllPoked`:** compared the under passage against the over passage's `cidBefore`, and threw a `NullPointerException` for any crossing without an over row.
- **`remPokesAndTwists`:** applied each removal to the original `fnList`, never to the result, so whenever there was a twist or poke the `while` loop never ended.

What they do now (same names and signatures):
- **`getAllTwisted(fnList)`:** crossings with a strand that leaves and comes straight back. Such a loop always bounds a one-strand region (its ends are the crossing's two different passages), so it's always removable.
- **`getAllPoked(fnList)`:** pairs of crossings bounding a two-strand region, where one strand is over at both ends and the other is under at both. Each pair is returned once, smaller id first. The region test uses `getOrientedBoundaries`, so two strands that merely join the same crossings without bounding an empty region aren't counted.
- **`remTwist(fnList, cid)` and `remPoke(fnList, cid1, cid2)`:**
  - They throw `IllegalArgumentException` if the crossing(s) aren't a twist / poke.
  - Both use one new helper, `removeCrossings`. It drops every passage through those crossings, joins the strand going in to the next passage kept (the same component walk `slideTriangle` uses), then renumbers strands and crossings so ids stay contiguous. That's what your unused `remPokeSetStrand` / `remPokeSetCid` were for, so those are gone.
  - A one-crossing loop untwists to an empty list (the unknot).
  - Removing a poke that would leave a link component with no crossings is refused, because FullNotation can't hold a component without crossings.
- **`remPokesAndTwists(fnList)`:** removes one twist (or, when there are none, one poke) at a time until nothing is left. It never edits its input.
- **Adding then removing a twist gives back exactly the original diagram,** ids included. A poke round trip gives back the same knot with the same number of crossings.

### Experiments
- **New request types:**
  - `RemTwistRequest(CrossingTypeDto location, Long numTimes)`: the location's `sign` optionally limits which twists.
  - `RemPokeRequest(Long numTimes)`: no location, because every poke has one positive and one negative crossing, so a sign filter wouldn't narrow anything.
  - `remPokesAndTwists` uses `NoArgsRequest`, like mirror and flip.
  - All three are registered in `MoveRequest`'s `@JsonSubTypes`.
- **`MoveRequestArgs`** gained `cid2` (`cid` and `cid2` are the poke's crossings).
- **`LocationSpecifyingService.convertRawRequestArgsAll`:**
  - A removal fits wherever there's a twist / poke to remove.
  - Pokes whose removal would be refused are skipped.
  - `remPokesAndTwists` only fits when there is something to remove, so a run without branching can tell when nothing changes any more.
- **`ExperimentsService`:**
  - `applyMove` handles the three new types.
  - With "everywhere at once" (`numTimes = 0`), removals keep removing until none fit: each removal renumbers the diagram, so locations found up front would go stale.
  - Branches record e.g. `cid=4` or `cid=2, cid2=5` as their move detail.

### `/api/moves` (Visual Component)
- `POST /removable` returns `{ twists: [cid…], pokes: [{x: cid1, y: cid2}…] }`.
- `POST /remTwist?cid=`, `POST /remPoke?cid1=&cid2=`, `POST /remPokesAndTwists`.
- **New `config/BadMoveHandler`:** a move's `IllegalArgumentException` (e.g. removing a twist that isn't there, or sliding a non-triangle) now comes back as a 400 with the reason. Before, it was a 500.

### Tests
- **New `RemovalMovesTest`** (7 tests):
  - the stored knots have nothing to remove
  - a twist round trip gives back the exact original
  - poke round trips keep the crossing count, consistency, planarity and determinant
  - `remPokesAndTwists` takes a trefoil with 2 twists and a poke back to 3 crossings, determinant 3
  - the one-crossing loop untwists to the unknot
  - removing something that isn't there is refused
  - a branching experiment on `remTwist` finds both orders of removing two twists
- **`MoveRequestJsonTest`** covers the three new JSON types.
- **`MovesControllerTest`** covers the new endpoints and the 400.
- **Backend total:** 49 run, 47 pass (the same 2 `BackendApplicationTests` failures as before).

---

## Round 3 changes (2026-11-11): hooking up the frontend

The frontend changes are in `../KnotResearch/FRONTEND_CHANGES.md`. Backend changes made so the frontend could use everything, plus two bugs found while testing it end to end:

### New / changed endpoints
- **`GET /api/knots/full_notation?knotId=`** returns the knot's stored FullNotation (from `full_notation_rolf`; 404 if there is none). The Visual Component draws and moves this, rather than rebuilding notation from the drawing.
- **`POST /api/moves/calculate_invariants`** now returns the `InvariantsDto`; it used to return nothing. The Visual Component uses it after every move.
- **`POST /api/moves/switch_placement?cid=`** is the crossing change.
- **`/api/moves/addTwist`:** `strand` and `sign` are explicit `@RequestParam`s.
- **`/api/moves/Slide`** uses `slideTriangle` when `boundary` holds three strands (Round 2).
- **`slideableList`** only returns triangles that `isSlideableTriangle` accepts, so every triangle it lists can actually be slid.
- **`GET /api/exp/experiment?experimentId=`** returns the experiment row and its status.
- **`GET /api/exp/state_notation?experimentId=&ogKnotId=&stateNum=&subStateNum=`** returns one state's diagram, so results can be drawn.
- **`run_experiment_no_goal`** now rejects an empty move list and sets the experiment status to `"Complete"` when done.

### `config/StringToLongPairConverter.java` (new)
- Lets `underStrandOriented=3,1` bind to `LongPair`. Without it `/api/moves/addPoke` couldn't read its parameters at all.

### `services/KnotService.java`
- Added `getFullNotation`.
- **`getGroupedKnots` no longer assumes `array_agg` comes back as `String[]`.** Postgres returns that, but other drivers such as H2 return `Object[]` or `java.sql.Array`, and the cast threw.

### Bug: the Alexander matrix was wrong for non-alternating diagrams (`InvariantCalculationsService`)
- **The problem:** `createAlexanderArcs` treated each over-passage as its own arc. An arc runs from one under-passage to the next and can pass *over* several crossings on the way. Whenever a strand went over two crossings in a row, one arc became two variables, and the under-strands were matched to whichever over-passage a `HashMap` returned first.
- **Why it went unnoticed:** stored Rolfsen diagrams are alternating, so they never hit this. Any twist does: in the Visual Component, twisting the trefoil showed Alexander 0 / determinant 0 instead of `t^2 - t + 1` / 3. Experiment invariants and goal checks after moves were affected the same way.
- **The fix:** walk each component of the knot and start a new arc after every under-passage.
  - A component that never passes under anything makes the link split, so it returns Alexander `0` / determinant 0 without calling SageMath.
  - An empty notation (the unknot) returns `1` / 1.
- **New `AlexanderMatrixTest`** checks that the determinant (evaluated at t = -1) is unchanged by every twist and poke on the trefoil and figure-eight, and that a crossing change unknots the trefoil.

### Bug: saddles chose pairs that make the diagram non-planar (`LocationSpecifyingService.identifyAllPossibleSaddles`)
- **The problem:** it paired a strand running *against* a region with one running *with* it. Walking around a region, those two point the same way across the plane, and `saddle`'s reconnection (strand1 → strand2's end, strand2 → strand1's end) can't be drawn in the plane for them.
- **How I checked:** I tried every pair on 9 diagrams (the three knots, each also poked and twisted).
  - All 77 opposite-direction pairs (the old rule) gave non-planar diagrams: regions ≠ crossings + 2.
  - Of the same-direction pairs, 149 stayed planar.
- **The fix:** pairs are now taken from strands running the *same* way around the region. `saddle` itself is unchanged.
- **New test `saddleChoicesKeepTheDiagramPlanar`.** `bothRequestsDeserializeSelectAndPersistOneMove` now saddles a trefoil triangle: the one-crossing loop has no valid saddle.
- **Known edge case, not fixed:** saddling the two strands on either side of a twist loop (e.g. S0 and S2 right after twisting S0) still gives a non-planar result. It was 6 of the 155 same-direction pairs tried. It would need a special rule; the frontend shows "can't be drawn" if it happens.

### Bug: mirror and flip couldn't be sent as JSON (`MoveRequest.java`)
- **The problem:** `@JsonTypeInfo(property = "type")` consumes `"type"` to pick the subtype and doesn't pass it on. `NoArgsRequest(MoveType type)` was therefore built with `type = null`, and `validateRequest` rejected it with a 400.
  - Every experiment containing a mirror or flip failed this way, which I found running one from the frontend.
  - The backend tests build `NoArgsRequest` directly, so they never saw it.
- **The fix:** `visible = true` (the record also receives `"type"`) plus `@JsonIgnoreProperties(ignoreUnknown = true)` (the other records, which have no `type` field, skip it).
- **New `MoveRequestJsonTest`** reads the exact JSON the frontend sends for all six move types.

### `application.properties`: error reasons reach the frontend
- `server.error.include-message=always`. Spring hides `ResponseStatusException` reasons by default, so the frontend could only show "Bad Request". It now shows reasons such as "Choose at least one goal invariant".

### `sage-math-service/app/main.py`: Alexander polynomial normalized
- **The problem:** the Alexander polynomial is only defined up to ±tᵏ, and SageMath returned whatever the minor gave (the trefoil came back as `-t^2 + t - 1`; the figure-eight as `t^3 - 3*t^2 + t`). Goals compare exact strings, so they could never match the stored values.
- **The fix:** the polynomial is divided by its lowest power of t and made to have a positive leading coefficient, e.g. `t^2 - t + 1`. That's the form the service's own figure-eight test already expected and the form the stored invariants use. A new test covers a shifted, negative result; the 5 Sage tests pass inside the container.
- The running SageMath container needs restarting (or `docker compose up --build sage-math`) to pick this up.

### Tests
- **Backend:** 40 run, 38 pass. The 2 failures are the same `BackendApplicationTests` ones as before (notes 2 and 3).
- **New:** `MovesControllerTest` (poke's `LongPair` parameters, crossing change, twist), `AlexanderMatrixTest` and `MoveRequestJsonTest`.

---

## Round 2 changes

### `services/ExperimentsService.java`
- **Restored the `while (!hitGoal)` loop.** It now repeats the whole move list until the goal is hit, with a limit of 1000 passes (`MAX_GOAL_ITERATIONS`).
  - `searchKnot` returns why it stopped (`StopReason`: goal reached / iteration limit / branch limit / no moves), and that sets the experiment status.
  - A new status: `"Stopped: 1000 Iterations Without Reaching Goal"`.
- **`numTimes` = n separate applications.** `expandSteps` turns each request into n `Step`s; each one is a level, and its location is found from the current diagram.
  - Without branching: `linearChild` applies the move once at the first fitting location, or `applyAllAtOnce` for `numTimes = 0`.
  - Round 1 instead applied all n inside one state when not branching.
- **Without branching, a pass that changes nothing stops the run.** If no move in the list fits anywhere, every later pass would do exactly the same, so it stops with "No Moves Available" rather than writing 1000 identical states.
- **Split `applyRequestLinear` into `applyRequestLinear` (n times) and `applyAllAtOnce`.** `runExperimentNoGoal` / `nextMoveSingleInstance` behave as before; they still save one state per request.
- **`applyMove` for slides calls `slideTriangle(fnList, args.getTriangleBdy())`.**
- **All-at-once slides skip a triangle that an earlier slide in the same batch broke up.** Overlapping triangles can't all be slid, and without the check the batch would throw.
- `describeArgs` includes the triangle.

### `services/LocationSpecifyingService.java`
- **New `identifyAllPossibleSlides`:** gets the triangles from `slideableList`, then keeps only proper triangles, drops duplicates, and applies the `SlideLoc` filters.
- `convertRawRequestArgsAll` now has a `SlideRequest` case that returns one `MoveRequestArgs` per triangle, with `triangleBdy` set.

### `models/dtos/experiments/SlideLoc.java`
- Changed from a class with private fields and no getters into a record `SlideLoc(CrossingTypeDto crossing, StrandTypeDto strand)`. Jackson couldn't fill the old class from JSON, and the location code couldn't read it.

### `services/FullNotationMovesService.java`
- **`switchPlacement` fixed:**
  - It now swaps over/under on both rows of the crossing.
  - It flips the sign: same orientations with over/under swapped means the opposite sign, as `mirror` does for every crossing.
  - It returns new rows instead of editing the input.
  - Before, the two `if`s ran one after the other (over → under → over again), so it did nothing, and the sign was never changed.
- **`Slide` rewritten** (your original is saved in `SLIDE_ORIGINAL.java.txt`).
  - **What was wrong:** I generated 84 small diagrams (2 pokes / twists from the loop) and tried every slideable triangle. The old `Slide` threw on most of them:
    - `ConcurrentModificationException`: the second loop added rows to `newFnList` while iterating over it.
    - `NullPointerException` and `IllegalArgumentException` on others.
    - On the rest it returned the diagram unchanged.
  - **The new version** uses the standard combinatorial form of Reidemeister III. On each of the triangle's three strands, the two crossings at its ends swap order along the knot. Every crossing keeps its placement and sign, every strand keeps its number, and all `cidBefore` / `cidAfter` links are rebuilt from the new order.
  - **New `slideTriangle(fnList, triangle)`** does the move from the triangle's three strands; this is what experiments use.
  - **`Slide(fnList, strand, cid)` keeps its signature** for `/Slide`. It finds the triangle formed by `strand` and the opposite crossing `cid`, then calls `slideTriangle`.
  - **New `isSlideableTriangle(fnList, triangle)`:** three different strands joining three different crossings pairwise, six different passages (no triangle strand runs straight into another), plus your `triangleBdySlideable` check.
    - 16 of the triangles that `slideableList` returns on small diagrams fail this. They're degenerate faces next to a twist loop, and sliding them isn't a real slide.
  - **Checked on every proper triangle in those diagrams (65):**
    - the result is consistent and still planar
    - it has the same crossings and writhe
    - it actually changed
    - the triangle is still a face
    - sliding again gives back the original diagram
    - `Slide(strand, cid)` gives the same result for every choice of strand
- **`addTwist` fixed (bug found while testing slides).**
  - The new twist crossing's two rows had `cidBefore` and `cidAfter` the wrong way round. `cidBefore` was set to the crossing at the *end* of the twisted strand, and `cidAfter` to the one at its *start*.
  - On the one-crossing loop both are the same crossing, which is why the existing tests passed. Any twist on a diagram with 2+ crossings produced broken links: 28 of the 84 test diagrams.
  - Anything that walks the knot by `cidBefore` / `cidAfter` (boundaries, so pokes, saddles and slides) could go wrong after a twist. The Alexander polynomial uses strands only and wasn't affected.
  - The fix swaps the two values in the four new-row lines.

### `controllers/MovesController.java`
- `/Slide`: if the request's `boundary` has 3 strands, that triangle is slid directly (`slideTriangle`). Otherwise it uses `strand` + `cid` as before.

### `SLIDE_ORIGINAL.java.txt` (new)
- Your original `Slide` method, so nothing is lost. Delete it whenever you like.

---

## Round 1 changes

### `services/ExperimentsService.java`
- Added `runExperimentWithGoal`, `getAllRelevantChoices` (one child per fitting location), and the per-knot breadth-first search (`searchKnot`, `Node`, `Child`, `SearchOutcome`).
  - The current level is kept in memory, so each level doesn't reload its diagrams from the DB.
- Every branch moves a copy of the parent diagram (`copyRows`), so siblings can't affect each other.
- `numTimes = null` now means "once". Before, `mirror`/`flip` (`NoArgsRequest`) always hit a `NullPointerException` (`i < req.numTimes()` with null).
- `nextMoveSingleInstance` takes `subStateNum` and continues that branch: `(stateNum, sub)` → `(stateNum + 1, sub)`.
  - It loads the current state with `findById` and reuses its invariants when nothing moved.
  - This replaces `getInvariantsFromStateNum`, whose JPQL no longer matched the entity.
- `nextMoveOnState` passes `subStateNum = 0` (this fixes a compile error).
- Added `saveState`, `requireState`, `markStatus`, `describeArgs` and status constants.

### `models/keys/ExperimentsFNKey.java`
- Added `subStateNum`. Without it, sibling substates at the same depth would overwrite each other's full-notation rows.

### `repositories/ExperimentsFNRepo.java`
- `findFnListFromState` also filters on `subStateNum`.

### `repositories/ExperimentStatesRepo.java`
- `findOgKnotIdsByExperimentIdAndStateNum` uses `DISTINCT`. Without it, a knot comes back once per substate.
- Removed `getInvariantsFromStateNum`; it used columns that no longer exist.
- Added `existsByIdExperimentIdAndIdStateNumGreaterThan` for the re-run guard.

### `models/entities/ExperimentStates.java`
- `invariants` is saved through `InvariantsConverter` as JSON in a `TEXT` column. JPA can't save a record holding a `Map` without one.
- Added `parentSubStateNum` and `moveDetail`, and a 4-argument constructor for start states.

### `models/converters/InvariantsConverter.java` (new)
- `InvariantsDto` ↔ JSON keyed by `InvariantType`, so new invariants need no schema change.

### `models/dtos/InvariantsDto.java`
- **Compact constructor:** drops nulls and coerces values to each `InvariantType`'s type. JSON gives back `Integer`, which wouldn't equal a computed `Long`, and goal matching would quietly fail.
- **Added an `InvariantsDto(String, Long, Long)` constructor** for the JPQL queries and tests.
- **Added `get(InvariantType)`.**

### `services/InvariantRolfService.java`
- Returns the DTO directly; the old accessors no longer exist.

### `models/dtos/experiments/MoveRequest.java`
- The `"slide"` JSON type was mapped to `AddPokeRequest`; it now maps to `SlideRequest`.

### `models/dtos/experiments/GoalExperimentRequest.java` (new) and `controllers/ExperimentsController.java`
- Added `POST /api/exp/run_experiment_with_goal` with body `moveRequests`, `goal`, `branch`.

---

## Tests

- **`ExperimentBranchingTest.java` (12 tests):**
  - one substate per twist location
  - stops at the first level that hits the goal
  - parent links across levels
  - unreachable branching goal stops at the branch limit (2 → 8 → 48 → 384 → 3840 states)
  - linear mode stays on substate 0
  - **repeats the list until the goal** (3 passes)
  - **numTimes = 2 applies the move twice, re-finding the location, before the next move**
  - **unreachable linear goal stops after 1000 iterations**
  - **stops early when no move fits**
  - goal already met at the start
  - re-run guard
  - endpoint JSON body
  - invariants converter round trip
- **`SlideAndCrossingChangeTest.java` (new, 6 tests):**
  - twists keep crossing links consistent (regression for the `addTwist` fix)
  - slide is a valid Reidemeister III on every proper triangle in the generated diagrams (all the checks listed above)
  - slide locations come from `slideableList`, and the filters work
  - slide JSON deserializes into `SlideRequest` / `SlideLoc`
  - an experiment can branch on slides and saves consistent diagrams
  - `switchPlacement` changes exactly one crossing, flips its sign, leaves the input alone, and undoes itself
- **`ExperimentMovesTest.java`, `InvariantCalculationsServiceTest.java`, `BackendApplicationTests.java`:** updated for the new signatures in round 1.

**Test results:** 28 of 30 pass. The 2 failures in `BackendApplicationTests` were already there (notes 2 and 3).

---

## Notes and open questions

1. **Database migration needed before deploying (important).** `ddl-auto=update` adds new columns but **never changes a primary key**. On Supabase the old primary keys don't include `sub_state_num`, so as soon as a branch makes a second substate at a depth, inserts would hit the old key and fail. Run something like this first (check the real constraint names with `\d experiment_states`):

   ```sql
   -- experiment_states
   ALTER TABLE experiment_states ADD COLUMN IF NOT EXISTS sub_state_num bigint;
   UPDATE experiment_states SET sub_state_num = 0 WHERE sub_state_num IS NULL;
   ALTER TABLE experiment_states ALTER COLUMN sub_state_num SET NOT NULL;
   ALTER TABLE experiment_states DROP CONSTRAINT experiment_states_pkey;
   ALTER TABLE experiment_states ADD PRIMARY KEY (experiment_id, og_knot_id, state_num, sub_state_num);

   -- copy the old invariant columns into the new JSON column
   ALTER TABLE experiment_states ADD COLUMN IF NOT EXISTS invariants text;
   UPDATE experiment_states
      SET invariants = json_build_object('Alexander_Polynomial', alexander_polynomial,
                                         'Determinant', determinant, 'Writhe', writhe)::text
    WHERE invariants IS NULL;

   -- experiments_full_notation
   ALTER TABLE experiments_full_notation ADD COLUMN IF NOT EXISTS sub_state_num bigint;
   UPDATE experiments_full_notation SET sub_state_num = 0 WHERE sub_state_num IS NULL;
   ALTER TABLE experiments_full_notation ALTER COLUMN sub_state_num SET NOT NULL;
   ALTER TABLE experiments_full_notation DROP CONSTRAINT experiments_full_notation_pkey;
   ALTER TABLE experiments_full_notation
     ADD PRIMARY KEY (experiment_id, og_knot_id, state_num, sub_state_num, placement, crossing_id);
   ```
   I did not run anything against the database. Once you've checked the copy into `invariants`, the old `alexander_polynomial` / `determinant` / `writhe` columns can be dropped.

2. **Fixed in Round 6. Was failing: `BackendApplicationTests.reloadsCompositeKeys...`.**
   - `ExperimentStates.moveType` has no `@Enumerated`, so it's stored as an ordinal, and Hibernate creates the column as `TINYINT`. H2 in PostgreSQL mode rejects `TINYINT`, so the `experiment_states` table is never created in tests.
   - Fixing it with `@Enumerated(EnumType.STRING)` would change how existing Supabase rows are read, so I left it for you to decide.
   - Also: because it's stored as an ordinal, any new `MoveType` (e.g. a crossing change) must be **added at the end** of the enum.

3. **Already failing: `BackendApplicationTests.rejectsMissingDataAndInvalidMovesOverHttp`.** It calls `/perform_move_single_state` and `/perform_move_all_in_state`, which are commented out in the controller, so it gets 404 instead of 400.

4. **"1000 iterations" = 1000 passes through the whole move list.** One pass applies every request in the list (each `numTimes` times). If you meant 1000 single moves instead, it's a one-line change in `searchKnot`.

5. **Branching with an unreachable goal** usually hits the 5000-states-per-level limit long before 1000 passes. Twists, for example, reach it at level 6. The status then says `"Stopped: Branch Limit Reached"`.

6. **`switchPlacement` isn't an experiment move yet.** To use it for un-knotting it needs:
   - a `MoveType` (added at the end of the enum, see note 2)
   - a request type (e.g. a `CrossingTypeDto` location to pick which crossings)
   - a "list every matching crossing" case in `convertRawRequestArgsAll`

   After that, branching on it works with this code as-is.

7. **No duplicate-diagram detection.** Two branches that produce the same diagram are both kept and both expanded. That includes a slide followed by the slide that undoes it, so slide-only branching revisits diagrams. It's correct but slower. Spotting duplicates needs a canonical form for full notation.

8. **Alexander polynomial goals compare exact strings,** so the goal has to be written exactly as SageMath formats it.

9. **`remTwist` looks wrong too (not changed):**
   - Its second `if` sets `cidBefore` / `strandBefore` where it looks like it should set `cidAfter` / `strandAfter`.
   - It edits the rows of the list it was given.
   - Nothing in experiments calls it yet.

10. **Credentials:** `src/main/resources/application.properties` has the Supabase password written in plaintext as the default value. It's probably worth rotating that password and keeping only the environment variable.
