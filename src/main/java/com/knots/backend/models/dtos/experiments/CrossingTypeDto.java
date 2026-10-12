package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.Placement;

public record CrossingTypeDto (
    Sign sign,
    Placement placement
) {}
