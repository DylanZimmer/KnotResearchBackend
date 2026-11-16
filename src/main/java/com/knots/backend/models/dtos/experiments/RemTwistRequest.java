package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.MoveType;

// Undo twists (Reidemeister I). location optionally limits which twists, by the crossing's sign
// (placement is ignored: every crossing has an over and an under passage).
public record RemTwistRequest(CrossingTypeDto location, Long numTimes) implements MoveRequest<CrossingTypeDto, Void> {
    @Override
    public MoveType type() {
        return MoveType.remTwist;
    }
}
