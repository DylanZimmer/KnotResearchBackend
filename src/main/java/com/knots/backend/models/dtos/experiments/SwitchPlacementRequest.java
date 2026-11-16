package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.MoveType;

// Switch Crossing Placement (crossing change): the strand that went over a crossing goes under,
// which flips the crossing's sign. location optionally limits which crossings, by sign (placement
// is ignored: every crossing has an over and an under passage).
public record SwitchPlacementRequest(CrossingTypeDto location, Long numTimes) implements MoveRequest<CrossingTypeDto, Void> {
    @Override
    public MoveType type() {
        return MoveType.switchPlacement;
    }
}
