package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.InvariantsDto;

import java.util.List;

// Body for /run_experiment_with_goal. goal lists only the invariants that have to match,
// e.g. {"values": {"Determinant": 1, "Alexander_Polynomial": "1"}}.
public record GoalExperimentRequest(
        List<MoveRequest<?, ?>> moveRequests,
        InvariantsDto goal,
        Boolean branch
) {}
