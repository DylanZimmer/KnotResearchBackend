package com.knots.backend.models.dtos.experiments;

import java.util.List;

public record BoundaryTypeDto(
        List<StrandTypeDto> strands,
        List<CrossingTypeDto> crossings,
        Integer numCrossings
) {}