package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.MoveType;
import com.knots.backend.models.dtos.Sign;

public record AddTwistRequest(StrandTypeDto location, Sign typeSpecificChoice, Long until) implements MoveRequest<StrandTypeDto, Sign> {
    @Override
    public MoveType type() {
        return MoveType.addTwist;
    }
}