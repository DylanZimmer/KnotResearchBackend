package com.knots.backend.services;

import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.dtos.MoveType;
import com.knots.backend.models.dtos.experiments.MoveRequest;
import com.knots.backend.models.dtos.MoveRequestArgs;
// import com.knots.backend.models.dtos.experiments.PokeRequest; // Disabled with unfinished request type.
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
import java.util.HashMap;
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
    private final LocationSpecifyingService locationSpecifyingService;

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
    public Long startExperiment(List<Long> knotIds) {
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
            eStatesList.add(new ExperimentStates(esKey, MoveType.startExperiment, "Starting", invariants.alexander_polynomial(), invariants.determinant(), invariants.writhe()));
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
        return experimentId;
    }

    @Transactional
    public void nextMoveSingleInstance(Long experimentId, Long ogKnotId, Long stateNum, MoveRequest<?, ?> req) {
        validateMove(stateNum);
        if (!experimentStatesRepo.existsById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found");
        }
        if (experimentStatesRepo.existsById(new ExperimentStatesKey(experimentId, ogKnotId, stateNum + 1))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Next experiment state already exists");
        }
        //Send it in as FullNotation, output it as FullNotation, put in the experiment specific
            //Info here. So when I do the diagram stuff I can call it the same way and convert
            //when I'm doing single knots
        List<FullNotation> fnListPlus = experimentsFNRepo.findFnListFromState(experimentId, ogKnotId, stateNum);
        HashMap<Long, Boolean> orientedStrands = fullNotationMovesService.orientAllStrands(fnListPlus);
        MoveRequestArgs requestArgs = locationSpecifyingService.convertRawRequestArgs(fnListPlus, req, orientedStrands);
        switch (req.type()) {
            case mirror ->
                fnListPlus = fullNotationMovesService.mirror(fnListPlus);
            case flip ->
                fnListPlus = fullNotationMovesService.orientationFlip(fnListPlus);
            case addTwist ->
                fnListPlus = fullNotationMovesService.addTwist(fnListPlus, requestArgs.getStrand(), requestArgs.getSign());
            case addPoke ->
                fnListPlus = fullNotationMovesService.addR2(fnListPlus, requestArgs.getUnderStrandOriented(), requestArgs.getOverStrandOriented());
            case slide ->
                fnListPlus = fullNotationMovesService.r3Slide(fnListPlus, requestArgs.getTriangleBdy(), requestArgs.getStrand(), requestArgs.getCid());
            case saddle ->
                fnListPlus = fullNotationMovesService.saddle(fnListPlus, requestArgs.getStrand1(), requestArgs.getStrand2());
        }
        InvariantsDto invariants = invariantCalculationsService.getAllInvariants(fnListPlus);
        Long nextStateNum = stateNum + 1;
        List<ExperimentsFullNotation> efnList = convertFnListToEfnList(fnListPlus, experimentId, ogKnotId, nextStateNum);
        experimentsFNRepo.saveAll(efnList);
        ExperimentStatesKey experimentStatesKey = new ExperimentStatesKey(experimentId, ogKnotId, nextStateNum);
        experimentStatesRepo.save(new ExperimentStates(experimentStatesKey, req.type(), "In Progress", invariants.alexander_polynomial(), invariants.determinant(), invariants.writhe()));
    }

    //Going to need to update experimentStates and experimentsFullNotation
    //General flow is to take in the previous state, get the efn and put it in ExperimentsFullNotation,
        //calculate the invariants, put them in experimentStates
    @Transactional
    //public void nextMoveOnState(Long experimentId, Long stateNum, MoveType move, MoveRequestArgs requestArgs) {
    public void nextMoveOnState(Long experimentId, Long stateNum, MoveRequest<?, ?> req) {
        List<Long> ogKnotIds = experimentStatesRepo.findOgKnotIdsByExperimentIdAndStateNum(experimentId, stateNum);
        if (ogKnotIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment state not found");
        }
        for (Long ogKnotId : ogKnotIds) {
            nextMoveSingleInstance(experimentId, ogKnotId, stateNum, req);
        }
    }

    private void validateMove(Long stateNum) {
        if (stateNum == null || stateNum < 0 || stateNum == Long.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state number");
        }
    }

    @Transactional
    public void runExperiment(Long experimentId, List<MoveRequest<?, ?>> moveRequests) {
        Long stateNum = 0L;
        for (MoveRequest<?, ?> request : moveRequests) {
            nextMoveOnState(experimentId, stateNum, request);
            stateNum++;
        }
    }

    public List<ExperimentStates> getExperimentStates(Long experimentId) {
        return experimentStatesRepo.findByIdExperimentId(experimentId);
    }

}
