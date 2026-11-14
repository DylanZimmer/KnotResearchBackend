package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.MoveType;

public record SaddleRequest(BoundaryTypeDto location, Long numTimes) implements MoveRequest<BoundaryTypeDto, Void> {
    @Override
    public MoveType type() {
        return MoveType.saddle;
    }
}
