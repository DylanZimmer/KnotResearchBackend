package com.knots.backend.services;

import com.knots.backend.models.dtos.InvariantsDto;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentsService {

    private final ExperimentsRepo experimentsRepo;
    private final FullNotationRolfRepo fullNotationRolfRepo;
    private final InvariantRolfRepo invariantRolfRepo;
    private final ExperimentsFNRepo experimentsFNRepo;
    private final ExperimentStatesRepo experimentStatesRepo;
    private final InvariantCalculationsService invariantCalculationsService;
    private final FullNotationMovesService fullNotationMovesService;

    private List<ExperimentsFullNotation> convertFnListToEfnList(List<FullNotation> fnList, Long experimentId, Long ogKnotId, Long stateNum) {
        return fnList.stream().map(fn -> {
                    ExperimentsFNKey key = new ExperimentsFNKey(
                            experimentId,
                            ogKnotId,
                            stateNum,
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
    public void startExperiment(List<Long> knotIds) {
        if (knotIds == null || knotIds.isEmpty() || knotIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one non-null knot ID");
        }
        knotIds = knotIds.stream().distinct().toList();
        Experiments experiment = new Experiments();
        experiment.setExperimentStatus("In Progress");
        experimentsRepo.save(experiment);
        Long experimentId = experiment.getExperimentId();

        List<ExperimentStates> eStatesList = new ArrayList<>();
        for (Long knotId : knotIds) {
            ExperimentStatesKey esKey = new ExperimentStatesKey(experimentId, knotId, 0L);
            InvariantsDto invariants = invariantRolfRepo.findByKnotId(knotId);
            if (invariants == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knot invariants not found: " + knotId);
            }
            eStatesList.add(new ExperimentStates(esKey, "start", "Starting", invariants.alexander_polynomial(), invariants.determinant(), invariants.writhe()));
        }
        experimentStatesRepo.saveAll(eStatesList);

        List<ExperimentsFullNotation> efnList = new ArrayList<>();
        for (Long knotId : knotIds) {
            List<FullNotationRolf> fnrList = fullNotationRolfRepo.findById_KnotId(knotId);
            for (FullNotationRolf fnr : fnrList) {
                ExperimentsFNKey efnKey = new ExperimentsFNKey(experimentId, knotId, 0L, fnr.getId().getPlacement(), fnr.getId().getCrossingId());
                efnList.add(new ExperimentsFullNotation(efnKey, fnr.getCidBefore(), fnr.getCidAfter(), fnr.getStrandBefore(), fnr.getStrandAfter(), fnr.getSign()));
            }
        }
        experimentsFNRepo.saveAll(efnList);
    }

    @Transactional
    public void nextMoveSingleInstance(Long experimentId, Long ogKnotId, Long stateNum, String move, MoveRequestArgs requestArgs) {
        validateMove(stateNum, move);
        if (!experimentStatesRepo.existsById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found");
        }
        if (experimentStatesRepo.existsById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum + 1))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Next experiment state already exists");
        }
        //Send it in as FullNotation, output it as FullNotation, put in the experiment specific
            //Info here. So when I do the diagram stuff I can call it the same way and convert
            //when I'm doing single knots
        List<FullNotation> fnList = experimentsFNRepo.findFnListFromState(experimentId, ogKnotId, stateNum);
        if (move.equals("mirror")) {
            fnList = fullNotationMovesService.mirror(fnList);
        } else if (move.equals("flipOrientation")) {
            fnList = fullNotationMovesService.orientationFlip(fnList);
        } else if (move.equals("addTwist")) {
            fnList = fullNotationMovesService.addTwist(fnList, requestArgs.getStrand(), requestArgs.getSign());
        }
        InvariantsDto invariants = invariantCalculationsService.getAllInvariants(fnList);
        Long nextStateNum = stateNum + 1;
        List<ExperimentsFullNotation> efnList = convertFnListToEfnList(fnList, experimentId, ogKnotId, nextStateNum);
        experimentsFNRepo.saveAll(efnList);
        ExperimentStatesKey experimentStatesKey = new ExperimentStatesKey(experimentId, ogKnotId, nextStateNum);
        experimentStatesRepo.save(new ExperimentStates(experimentStatesKey, move, "In Progress", invariants.alexander_polynomial(), invariants.determinant(), invariants.writhe()));
    }

    //Going to need to update experimentStates and experimentsFullNotation
    //General flow is to take in the previous state, get the efn and put it in ExperimentsFullNotation,
        //calculate the invariants, put them in experimentStates
    @Transactional
    public void nextMoveOnState(Long experimentId, Long stateNum, String move, MoveRequestArgs requestArgs) {
        validateMove(stateNum, move);
        List<Long> ogKnotIds = experimentStatesRepo.findOgKnotIdsByExperimentIdAndStateNum(experimentId, stateNum);
        if (ogKnotIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found");
        }
        for (Long ogKnotId : ogKnotIds) {
            nextMoveSingleInstance(experimentId, ogKnotId, stateNum, move, requestArgs);
        }
    }

    private void validateMove(Long stateNum, String move) {
        if (stateNum == null || stateNum < 0 || stateNum == Long.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state number");
        }
        if (!"mirror".equals(move) && !"flipOrientation".equals(move) && !"addTwist".equals(move)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported move");
        }
    }

}
