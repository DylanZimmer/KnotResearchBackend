package com.knots.backend.models.dtos.experiments;

public record StrandTypeDto(
        CrossingTypeDto crossing_before,
        CrossingTypeDto crossing_after,
        Boolean orientedWith
) {}