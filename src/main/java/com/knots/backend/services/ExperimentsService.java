package com.knots.backend.services;

import com.knots.backend.models.dtos.InvariantType;
import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.dtos.MoveType;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.experiments.MoveRequest;
import com.knots.backend.models.dtos.MoveRequestArgs;
import com.knots.backend.models.entities.*;
import com.knots.backend.models.keys.ExperimentStatesKey;
import com.knots.backend.models.keys.ExperimentsFNKey;
import com.knots.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class ExperimentsService {

    // State statuses
    static final String STATUS_STARTING = "Starting";
    static final String STATUS_IN_PROGRESS = "In Progress";
    static final String STATUS_GOAL_REACHED = "Goal Reached";
    static final String STATUS_NO_MOVES = "No Moves Available";

    // Experiment statuses written by runExperimentWithGoal
    static final String EXPERIMENT_COMPLETE = "Complete";
    static final String EXPERIMENT_GOAL_REACHED = "Goal Reached";
    static final String EXPERIMENT_GOAL_PARTIALLY_REACHED = "Goal Reached For Some Knots";
    static final String EXPERIMENT_GOAL_NOT_REACHED = "Goal Not Reached";
    static final String EXPERIMENT_BRANCH_LIMIT = "Stopped: Branch Limit Reached";
    static final String EXPERIMENT_ITERATION_LIMIT = "Stopped: 1000 Iterations Without Reaching Goal";

    private static final Set<MoveType> REMOVALS = EnumSet.of(MoveType.remTwist, MoveType.remPoke, MoveType.remPokesAndTwists);

    // Most passes through the move list runExperimentWithGoal makes for one knot.
    static final int MAX_GOAL_ITERATIONS = 1000;

    // Upper bound on states in one level of a branching search, per knot. Branching grows
    // exponentially and each new state costs a SageMath call, so stop before it runs away.
    static final int MAX_STATES_PER_LEVEL = 5000;

    private final ExperimentsRepo experimentsRepo;
    private final FullNotationRolfRepo fullNotationRolfRepo;
    private final InvariantRolfRepo invariantRolfRepo;
    private final ExperimentsFNRepo experimentsFNRepo;
    private final ExperimentStatesRepo experimentStatesRepo;
    private final InvariantCalculationsService invariantCalculationsService;
    private final FullNotationMovesService fullNotationMovesService;
    private final LocationSpecifyingService locationSpecifyingService;

    private List<ExperimentsFullNotation> convertFnListToEfnList(List<FullNotation> fnList, Long experimentId, Long ogKnotId, Long stateNum, Long subStateNum) {
        return fnList.stream().map(fn -> {
                    ExperimentsFNKey key = new ExperimentsFNKey(
                            experimentId,
                            ogKnotId,
                            stateNum,
                            subStateNum,
                            fn.getPlacement(),
                            fn.getCrossingId()
                    );
                    return new ExperimentsFullNotation(
                            key,
                            fn.getCidBefore(),
                            fn.getCidAfter(),
                            fn.getStrandBefore(),
                            fn.getStrandAfter(),
                            fn.getSign()
                    );
                })
                .toList();
    }

    @Transactional
    public Long startExperiment(List<Long> knotIds) {
        if (knotIds == null || knotIds.isEmpty() || knotIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one non-null knot ID");
        }
        knotIds = knotIds.stream().distinct().toList();
        Experiments experiment = new Experiments();
        experiment.setExperimentStatus(STATUS_IN_PROGRESS);
        experimentsRepo.save(experiment);
        Long experimentId = experiment.getExperimentId();

        List<ExperimentStates> eStatesList = new ArrayList<>();
        for (Long knotId : knotIds) {
            ExperimentStatesKey esKey = new ExperimentStatesKey(experimentId, knotId, 0L, 0L);
            InvariantsDto invariants = invariantRolfRepo.findByKnotId(knotId);
            if (invariants == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knot invariants not found: " + knotId);
            }
            eStatesList.add(new ExperimentStates(esKey, MoveType.startExperiment, STATUS_STARTING, invariants));
        }
        experimentStatesRepo.saveAll(eStatesList);

        List<ExperimentsFullNotation> efnList = new ArrayList<>();
        for (Long knotId : knotIds) {
            List<FullNotationRolf> fnrList = fullNotationRolfRepo.findById_KnotId(knotId);
            for (FullNotationRolf fnr : fnrList) {
                ExperimentsFNKey efnKey = new ExperimentsFNKey(experimentId, knotId, 0L, 0L, fnr.getId().getPlacement(), fnr.getId().getCrossingId());
                efnList.add(new ExperimentsFullNotation(efnKey, fnr.getCidBefore(), fnr.getCidAfter(), fnr.getStrandBefore(), fnr.getStrandAfter(), fnr.getSign()));
            }
        }
        experimentsFNRepo.saveAll(efnList);
        return experimentId;
    }

    //****************************************************************************
    // Linear (non-branching) moves

    // Applies req to (stateNum, subStateNum) and saves the result as (stateNum + 1, subStateNum),
    // i.e. it continues the same branch one step further.
    @Transactional
    public void nextMoveSingleInstance(Long experimentId, Long ogKnotId, Long stateNum, Long subStateNum, MoveRequest<?, ?> req) {
        validateMove(stateNum);
        validateRequest(req);
        ExperimentStates current = requireState(experimentId, ogKnotId, stateNum, subStateNum);
        if (experimentStatesRepo.existsById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum + 1, subStateNum))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Next experiment state already exists");
        }
        //Send it in as FullNotation, output it as FullNotation, put in the experiment specific
            //Info here. So when I do the diagram stuff I can call it the same way and convert
            //when I'm doing single knots
        List<FullNotation> fnList = experimentsFNRepo.findFnListFromState(experimentId, ogKnotId, stateNum, subStateNum);
        LinearResult result = applyRequestLinear(fnList, req);
        InvariantsDto invariants = result.timesApplied() > 0
                ? invariantCalculationsService.getAllInvariants(result.fnList())
                : current.getInvariants();
        saveState(experimentId, ogKnotId, stateNum + 1, subStateNum, subStateNum, req.type(), STATUS_IN_PROGRESS,
                describeLinear(result), result.fnList(), invariants);
    }

    private record LinearResult(List<FullNotation> fnList, int timesApplied) {}

    // numTimes == 0: apply at every fitting location at once. numTimes == n: apply n times,
    // re-finding a location each time. Never mutates fnList.
    private LinearResult applyRequestLinear(List<FullNotation> fnList, MoveRequest<?, ?> req) {
        if (Long.valueOf(0L).equals(req.numTimes())) {
            return applyAllAtOnce(fnList, req);
        }
        List<FullNotation> fnListPlus = copyRows(fnList);
        int timesApplied = 0;
        long times = req.numTimes() == null ? 1L : req.numTimes();
        for (long i = 0; i < times; i++) {
            HashMap<Long, Boolean> orientedStrands = fullNotationMovesService.orientAllStrands(fnListPlus);
            MoveRequestArgs args = locationSpecifyingService.convertRawRequestArgsSingle(fnListPlus, req, orientedStrands);
            if (args == null) {
                break;
            }
            fnListPlus = applyMove(fnListPlus, req, args);
            timesApplied++;
        }
        return new LinearResult(fnListPlus, timesApplied);
    }

    // Applies req at every location it fits in fnList, all found before any move is made.
    // Removals renumber strands and crossings, so for them the locations are found again after each
    // one, until none is left.
    private LinearResult applyAllAtOnce(List<FullNotation> fnList, MoveRequest<?, ?> req) {
        if (REMOVALS.contains(req.type())) {
            List<FullNotation> current = copyRows(fnList);
            int removed = 0;
            MoveRequestArgs args;
            while ((args = locationSpecifyingService.convertRawRequestArgsSingle(current, req, fullNotationMovesService.orientAllStrands(current))) != null) {
                current = applyMove(current, req, args);
                removed++;
            }
            return new LinearResult(current, removed);
        }
        List<FullNotation> fnListPlus = copyRows(fnList);
        int timesApplied = 0;
        List<MoveRequestArgs> batch = locationSpecifyingService.convertRawRequestArgsAll(fnListPlus, req, fullNotationMovesService.orientAllStrands(fnListPlus));
        for (int i = 0; i < batch.size(); i++) {
            MoveRequestArgs args = batch.get(i);
            // An earlier slide in the batch can break up a later, overlapping triangle; skip those.
            if (req.type() == MoveType.slide && !fullNotationMovesService.isSlideableTriangle(fnListPlus, args.getTriangleBdy())) {
                continue;
            }
            fnListPlus = applyMove(fnListPlus, req, args);
            timesApplied++;
            // Later requests still refer to the original diagram. Track renumbering
            // while keeping each request on the first segment of its original strand.
            for (int j = i + 1; j < batch.size(); j++) {
                MoveRequestArgs pending = batch.get(j);
                if (req.type() == MoveType.addTwist) {
                    if (pending.getStrand() > args.getStrand()) {
                        pending.setStrand(pending.getStrand() + 2);
                    }
                } else if (req.type() == MoveType.addPoke) {
                    pending.setUnderStrandOriented(shiftPokeStrand(pending.getUnderStrandOriented(), args));
                    pending.setOverStrandOriented(shiftPokeStrand(pending.getOverStrandOriented(), args));
                }
            }
        }
        return new LinearResult(fnListPlus, timesApplied);
    }

    private LongPair shiftPokeStrand(
            LongPair strand, MoveRequestArgs applied) {
        long shift = (strand.x() > applied.getUnderStrandOriented().x() ? 2L : 0L)
                + (strand.x() > applied.getOverStrandOriented().x() ? 2L : 0L);
        return new LongPair(strand.x() + shift, strand.y());
    }

    private List<FullNotation> applyMove(List<FullNotation> fnList, MoveRequest<?, ?> req, MoveRequestArgs args) {
        return switch (req.type()) {
            case mirror -> fullNotationMovesService.mirror(fnList);
            case flip -> fullNotationMovesService.orientationFlip(fnList);
            case addTwist -> fullNotationMovesService.addTwist(fnList, args.getStrand(), args.getSign());
            case addPoke -> fullNotationMovesService.addPoke(fnList, args.getUnderStrandOriented(), args.getOverStrandOriented());
            case slide -> fullNotationMovesService.slideTriangle(fnList, args.getTriangleBdy());
            case saddle -> fullNotationMovesService.saddle(fnList, args.getStrand1(), args.getStrand2());
            case remTwist -> fullNotationMovesService.remTwist(fnList, args.getCid());
            case remPoke -> fullNotationMovesService.remPoke(fnList, args.getCid(), args.getCid2());
            case remPokesAndTwists -> fullNotationMovesService.remPokesAndTwists(fnList);
            case switchPlacement -> fullNotationMovesService.switchPlacement(fnList, args.getCid());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported experiment move");
        };
    }

    //Going to need to update experimentStates and experimentsFullNotation
    //General flow is to take in the previous state, get the efn and put it in ExperimentsFullNotation,
        //calculate the invariants, put them in experimentStates
    // Linear runs only use subState 0.
    @Transactional
    public void nextMoveOnState(Long experimentId, Long stateNum, MoveRequest<?, ?> req) {
        List<Long> ogKnotIds = experimentStatesRepo.findOgKnotIdsByExperimentIdAndStateNum(experimentId, stateNum);
        if (ogKnotIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found");
        }
        for (Long ogKnotId : ogKnotIds) {
            nextMoveSingleInstance(experimentId, ogKnotId, stateNum, 0L, req);
        }
    }

    private void validateMove(Long stateNum) {
        if (stateNum == null || stateNum < 0 || stateNum == Long.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state number");
        }
    }

    private void validateRequest(MoveRequest<?, ?> req) {
        if (req == null || req.type() == null || (req.numTimes() != null && req.numTimes() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid move request or numTimes count");
        }
    }

    @Transactional
    public void runExperimentNoGoal(Long experimentId, List<MoveRequest<?, ?>> moveRequests) {
        if (moveRequests == null || moveRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one move request");
        }
        Experiments experiment = getExperiment(experimentId);
        Long stateNum = 0L;
        for (MoveRequest<?, ?> request : moveRequests) {
            nextMoveOnState(experimentId, stateNum, request);
            stateNum++;
        }
        experiment.setExperimentStatus(EXPERIMENT_COMPLETE);
        experimentsRepo.save(experiment);
    }

    //****************************************************************************
    // Goal-directed runs, optionally branching

    // One state in the current level of the search, kept in memory so the next level
    // doesn't have to reload it.
    private record Node(Long subStateNum, List<FullNotation> fnList, InvariantsDto invariants) {}

    // A candidate next state, made from parent by one move.
    private record Child(Node parent, List<FullNotation> fnList, boolean changed, String moveDetail) {}

    // One saved level of the run: a single application of req, or, when allAtOnce is set
    // (numTimes = 0 without branching), req applied at every fitting location at once.
    private record Step(MoveRequest<?, ?> req, boolean allAtOnce) {}

    // Every way to apply req once to the parent diagram: one child per fitting location
    // (e.g. one per strand that matches an addTwist location). Locations are found from
    // this diagram, so they are recalculated after every move.
    private List<Child> getAllRelevantChoices(Node parent, MoveRequest<?, ?> req) {
        List<FullNotation> fnList = parent.fnList();
        HashMap<Long, Boolean> orientedStrands = fullNotationMovesService.orientAllStrands(fnList);
        List<MoveRequestArgs> allArgs = locationSpecifyingService.convertRawRequestArgsAll(fnList, req, orientedStrands);
        List<Child> children = new ArrayList<>();
        for (MoveRequestArgs args : allArgs) {
            // Copy first so every sibling starts from the same parent diagram.
            List<FullNotation> moved = applyMove(copyRows(fnList), req, args);
            children.add(new Child(parent, moved, true, describeArgs(args)));
        }
        return children;
    }

    // Without branching: apply the step once at the first fitting location (or everywhere at once).
    // If it fits nowhere, the child is the parent diagram unchanged.
    private Child linearChild(Node parent, Step step) {
        if (step.allAtOnce()) {
            LinearResult result = applyAllAtOnce(parent.fnList(), step.req());
            return new Child(parent, result.fnList(), result.timesApplied() > 0, describeLinear(result));
        }
        List<FullNotation> fnList = copyRows(parent.fnList());
        MoveRequestArgs args = locationSpecifyingService.convertRawRequestArgsSingle(
                fnList, step.req(), fullNotationMovesService.orientAllStrands(fnList));
        if (args == null) {
            return new Child(parent, fnList, false, "no fitting location");
        }
        return new Child(parent, applyMove(fnList, step.req(), args), true, describeArgs(args));
    }

    // numTimes = n: the move is applied n times (each its own level, location recalculated each
    // time) before going on to the next request. null means once. numTimes = 0 means "every
    // fitting location at once" without branching, and one level of branches with branching.
    private List<Step> expandSteps(List<MoveRequest<?, ?>> moveRequests, boolean branch) {
        List<Step> steps = new ArrayList<>();
        for (MoveRequest<?, ?> req : moveRequests) {
            if (Long.valueOf(0L).equals(req.numTimes())) {
                steps.add(new Step(req, !branch));
                continue;
            }
            long times = req.numTimes() == null ? 1L : req.numTimes();
            for (long i = 0; i < times; i++) {
                steps.add(new Step(req, false));
            }
        }
        return steps;
    }

    // Repeats moveRequests (each move numTimes times) from state 0 of every knot in the experiment
    // until a state's invariants match every invariant given in goal, or MAX_GOAL_ITERATIONS passes
    // through the list have run.
    //   branch = false: one path on subState 0.
    //   branch = true: breadth-first tree. Each state gets one child substate per location the next
    //     move fits. Each knot stops after the first level where any branch hits the goal, so every
    //     returned state uses the minimum number of moves.
    // Returns the states that reached the goal.
    @Transactional
    public List<ExperimentStates> runExperimentWithGoal(Long experimentId, List<MoveRequest<?, ?>> moveRequests, InvariantsDto goal, Boolean branch) {
        if (moveRequests == null || moveRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one move request");
        }
        moveRequests.forEach(this::validateRequest);
        if (goal == null || goal.values().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one goal invariant");
        }
        Experiments experiment = experimentsRepo.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));
        if (experimentStatesRepo.existsByIdExperimentIdAndIdStateNumGreaterThan(experimentId, 0L)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Experiment has already been run; start a new one");
        }
        List<Long> ogKnotIds = experimentStatesRepo.findOgKnotIdsByExperimentIdAndStateNum(experimentId, 0L);
        if (ogKnotIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found");
        }

        boolean doBranch = Boolean.TRUE.equals(branch);
        List<Step> steps = expandSteps(moveRequests, doBranch);

        List<ExperimentStates> goalStates = new ArrayList<>();
        int knotsReached = 0;
        boolean branchLimitHit = false;
        boolean iterationLimitHit = false;
        for (Long ogKnotId : ogKnotIds) {
            SearchOutcome outcome = searchKnot(experimentId, ogKnotId, steps, goal, doBranch);
            goalStates.addAll(outcome.goalStates());
            if (outcome.stop() == StopReason.GOAL_REACHED) {
                knotsReached++;
            }
            branchLimitHit |= outcome.stop() == StopReason.BRANCH_LIMIT;
            iterationLimitHit |= outcome.stop() == StopReason.ITERATION_LIMIT;
        }

        if (knotsReached == ogKnotIds.size()) {
            experiment.setExperimentStatus(EXPERIMENT_GOAL_REACHED);
        } else if (knotsReached > 0) {
            experiment.setExperimentStatus(EXPERIMENT_GOAL_PARTIALLY_REACHED);
        } else if (branchLimitHit) {
            experiment.setExperimentStatus(EXPERIMENT_BRANCH_LIMIT);
        } else if (iterationLimitHit) {
            experiment.setExperimentStatus(EXPERIMENT_ITERATION_LIMIT);
        } else {
            experiment.setExperimentStatus(EXPERIMENT_GOAL_NOT_REACHED);
        }
        experimentsRepo.save(experiment);
        return goalStates;
    }

    enum StopReason { GOAL_REACHED, ITERATION_LIMIT, BRANCH_LIMIT, NO_MOVES }

    private record SearchOutcome(List<ExperimentStates> goalStates, StopReason stop, int iterations) {}

    private SearchOutcome searchKnot(Long experimentId, Long ogKnotId, List<Step> steps, InvariantsDto goal, boolean doBranch) {
        ExperimentStates root = requireState(experimentId, ogKnotId, 0L, 0L);
        if (goalMet(goal, root.getInvariants())) {
            root.setExperimentStatus(STATUS_GOAL_REACHED);
            experimentStatesRepo.save(root);
            return new SearchOutcome(List.of(root), StopReason.GOAL_REACHED, 0);
        }

        List<Node> frontier = List.of(new Node(0L,
                experimentsFNRepo.findFnListFromState(experimentId, ogKnotId, 0L, 0L), root.getInvariants()));
        List<ExperimentStates> goalStates = new ArrayList<>();
        long stateNum = 0L;
        boolean hitGoal = false;
        int iterations = 0;

        while (!hitGoal && iterations < MAX_GOAL_ITERATIONS) {
            iterations++;
            boolean changedThisPass = false;
            for (Step step : steps) {
                // Build the whole next level first so the size limit is checked before any SageMath calls.
                List<Child> children = new ArrayList<>();
                for (Node node : frontier) {
                    List<Child> nodeChildren = doBranch ? getAllRelevantChoices(node, step.req()) : List.of(linearChild(node, step));
                    if (nodeChildren.isEmpty()) {
                        markStatus(experimentId, ogKnotId, stateNum, node.subStateNum(), STATUS_NO_MOVES);
                    }
                    children.addAll(nodeChildren);
                    if (children.size() > MAX_STATES_PER_LEVEL) {
                        return new SearchOutcome(goalStates, StopReason.BRANCH_LIMIT, iterations);
                    }
                }

                long nextStateNum = stateNum + 1;
                List<Node> nextFrontier = new ArrayList<>();
                long subStateNum = 0L;
                for (Child child : children) {
                    InvariantsDto invariants = child.changed()
                            ? invariantCalculationsService.getAllInvariants(child.fnList())
                            : child.parent().invariants();
                    boolean hit = goalMet(goal, invariants);
                    ExperimentStates saved = saveState(experimentId, ogKnotId, nextStateNum, subStateNum,
                            child.parent().subStateNum(), step.req().type(), hit ? STATUS_GOAL_REACHED : STATUS_IN_PROGRESS,
                            child.moveDetail(), child.fnList(), invariants);
                    if (hit) {
                        goalStates.add(saved);
                        hitGoal = true;
                    }
                    changedThisPass |= child.changed();
                    nextFrontier.add(new Node(subStateNum, child.fnList(), invariants));
                    subStateNum++;
                }

                frontier = nextFrontier;
                stateNum = nextStateNum;
                // The whole level is finished before stopping, so all minimal solutions are recorded.
                if (hitGoal) {
                    return new SearchOutcome(goalStates, StopReason.GOAL_REACHED, iterations);
                }
                if (frontier.isEmpty()) {
                    return new SearchOutcome(goalStates, StopReason.NO_MOVES, iterations);
                }
            }
            // Without branching, a full pass where no move fit anywhere leaves the diagram as it was,
            // so every later pass would do the same. Stop instead of repeating it 1000 times.
            if (!doBranch && !changedThisPass) {
                markStatus(experimentId, ogKnotId, stateNum, 0L, STATUS_NO_MOVES);
                return new SearchOutcome(goalStates, StopReason.NO_MOVES, iterations);
            }
        }
        return new SearchOutcome(goalStates, StopReason.ITERATION_LIMIT, iterations);
    }

    // Every invariant given in goal must equal the state's value. Invariants left out of goal are ignored.
    private boolean goalMet(InvariantsDto goal, InvariantsDto invariants) {
        if (invariants == null) {
            return false;
        }
        for (Map.Entry<InvariantType, Object> target : goal.values().entrySet()) {
            if (!Objects.equals(target.getValue(), invariants.get(target.getKey()))) {
                return false;
            }
        }
        return true;
    }

    //****************************************************************************
    // Helpers

    private ExperimentStates requireState(Long experimentId, Long ogKnotId, Long stateNum, Long subStateNum) {
        return experimentStatesRepo.findById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum, subStateNum))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found"));
    }

    private void markStatus(Long experimentId, Long ogKnotId, Long stateNum, Long subStateNum, String status) {
        experimentStatesRepo.findById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum, subStateNum))
                .ifPresent(state -> {
                    state.setExperimentStatus(status);
                    experimentStatesRepo.save(state);
                });
    }

    private ExperimentStates saveState(Long experimentId, Long ogKnotId, Long stateNum, Long subStateNum, Long parentSubStateNum,
                                       MoveType moveType, String status, String moveDetail,
                                       List<FullNotation> fnList, InvariantsDto invariants) {
        experimentsFNRepo.saveAll(convertFnListToEfnList(fnList, experimentId, ogKnotId, stateNum, subStateNum));
        ExperimentStates state = new ExperimentStates(new ExperimentStatesKey(experimentId, ogKnotId, stateNum, subStateNum),
                moveType, status, invariants, parentSubStateNum, moveDetail);
        experimentStatesRepo.save(state);
        return state;
    }

    private List<FullNotation> copyRows(List<FullNotation> fnList) {
        return fnList.stream()
                .map(fn -> new FullNotation(fn.getKnotId(), fn.getCrossingId(), fn.getPlacement(),
                        fn.getCidBefore(), fn.getCidAfter(), fn.getStrandBefore(), fn.getStrandAfter(), fn.getSign()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String describeArgs(MoveRequestArgs args) {
        StringJoiner joiner = new StringJoiner(", ");
        if (args.getStrand() != null) joiner.add("strand=" + args.getStrand());
        if (args.getSign() != null) joiner.add("sign=" + args.getSign());
        if (args.getUnderStrandOriented() != null) joiner.add("under=" + args.getUnderStrandOriented());
        if (args.getOverStrandOriented() != null) joiner.add("over=" + args.getOverStrandOriented());
        if (args.getCid() != null) joiner.add("cid=" + args.getCid());
        if (args.getCid2() != null) joiner.add("cid2=" + args.getCid2());
        if (args.getStrand1() != null) joiner.add("strand1=" + args.getStrand1());
        if (args.getStrand2() != null) joiner.add("strand2=" + args.getStrand2());
        if (args.getTriangleBdy() != null) joiner.add("triangle=" + args.getTriangleBdy());
        return joiner.length() == 0 ? null : joiner.toString();
    }

    private String describeLinear(LinearResult result) {
        return "applied " + result.timesApplied() + " time(s)";
    }

    public List<ExperimentStates> getExperimentStates(Long experimentId) {
        return experimentStatesRepo.findByIdExperimentId(experimentId);
    }

    public Experiments getExperiment(Long experimentId) {
        return experimentsRepo.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));
    }

    public List<FullNotation> getStateNotation(Long experimentId, Long ogKnotId, Long stateNum, Long subStateNum) {
        requireState(experimentId, ogKnotId, stateNum, subStateNum);
        return experimentsFNRepo.findFnListFromState(experimentId, ogKnotId, stateNum, subStateNum);
    }

}
