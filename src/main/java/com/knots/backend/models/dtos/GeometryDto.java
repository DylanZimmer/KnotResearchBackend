package com.knots.backend.models.dtos;

import java.util.List;

public record GeometryDto (
        List<List<Long>> vertex_positions,
        List<List<Long>> arrows,
        List<List<Long>> crossing_specs,
        String handedness
) {}
