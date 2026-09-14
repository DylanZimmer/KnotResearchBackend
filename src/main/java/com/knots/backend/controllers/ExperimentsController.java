package com.knots.backend.controllers;


import com.knots.backend.models.dtos.MoveRequestArgs;
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
    public void startExperiment(@RequestBody List<Long> knotIds) {
        experimentsService.startExperiment(knotIds);
    }

    //MoveRequest will contain experimentId, ogKnotId, stateNum, move, additional parameters for the move
    @PostMapping("/perform_move_single_instance")
    public void nextMoveSingleInstance(@RequestParam Long experimentId, @RequestParam Long ogKnotId, @RequestParam Long stateNum, @RequestParam String move, @RequestBody MoveRequestArgs requestArgs) {
        experimentsService.nextMoveSingleInstance(experimentId, ogKnotId, stateNum, move, requestArgs);
    }

    //MoveRequest will contain experimentId, stateNum, move. Potentially ways to find the additional parameters needed
    @PostMapping("/perform_move_for_state")
    public void nextMoveOnState(@RequestParam Long experimentId, @RequestParam Long stateNum, @RequestParam String move, @RequestBody MoveRequestArgs requestArgs) {
        experimentsService.nextMoveOnState(experimentId, stateNum, move, requestArgs);
    }

    //Needs to be created to get the strand from the information about crossings/strands given by user choices
    /*
    @GetMapping("/get_fn_strand")
    public Long getFnStrandFromProperties(@RequestParam )
    */
}
