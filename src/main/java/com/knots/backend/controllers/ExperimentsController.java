package com.knots.backend.controllers;


import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.MoveRequestArgs;
import com.knots.backend.models.dtos.MoveType;
import com.knots.backend.models.dtos.experiments.MoveRequest;
import com.knots.backend.models.entities.ExperimentStates;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.services.ExperimentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exp")
@RequiredArgsConstructor
public class ExperimentsController {

    private final ExperimentsService experimentsService;

    @PostMapping("/start_experiment")
    public Long startExperiment(@RequestBody List<Long> knotIds) {
        return experimentsService.startExperiment(knotIds);
    }

    @PostMapping("/run_experiment")
    public void runExperiment(@RequestParam Long experimentId, @RequestBody List<MoveRequest<?, ?>> reqs) {
        experimentsService.runExperiment(experimentId, reqs);
    }

    @GetMapping("/experiment_states")
    public List<ExperimentStates> getExperimentStates(@RequestParam Long experimentId) {
        return experimentsService.getExperimentStates(experimentId);
    }



    /*
    //MoveRequest will contain experimentId, ogKnotId, stateNum, move, additional parameters for the move
    @PostMapping("/perform_move_single_instance")
    public void nextMoveSingleInstance(@RequestParam Long experimentId, @RequestParam Long ogKnotId, @RequestParam Long stateNum, @RequestParam MoveType move, @RequestBody MoveRequest req) {
        experimentsService.nextMoveSingleInstance(experimentId, ogKnotId, stateNum, req);
    }

    //MoveRequest will contain experimentId, stateNum, move. Potentially ways to find the additional parameters needed
    @PostMapping("/perform_move_for_state")
    public void nextMoveOnState(@RequestParam Long experimentId, @RequestParam Long stateNum, @RequestBody MoveRequest req) {
        experimentsService.nextMoveOnState(experimentId, stateNum, req);
    }
    */
}
