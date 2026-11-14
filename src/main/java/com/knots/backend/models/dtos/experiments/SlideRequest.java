package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.MoveType;

public record SlideRequest(SlideLoc location, Long numTimes) implements MoveRequest<SlideLoc, Void> {
    @Override
    public MoveType type() {
        return MoveType.slide;
    }
}
