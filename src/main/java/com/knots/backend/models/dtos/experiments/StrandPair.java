package com.knots.backend.models.dtos.experiments;

public record StrandPair(
        StrandTypeDto strand1,
        StrandTypeDto strand2
) {}
