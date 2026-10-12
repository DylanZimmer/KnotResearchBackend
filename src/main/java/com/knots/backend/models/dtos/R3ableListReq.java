package com.knots.backend.models.dtos;

import com.knots.backend.models.entities.FullNotation;

import java.util.List;

public record R3ableListReq(
        List<FullNotation> fnList,
        List<List<Long>> boundaries
) {}
