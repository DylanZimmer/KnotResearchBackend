package com.knots.backend.models.dtos;

import com.knots.backend.models.entities.FullNotation;

import java.util.List;

public record SlideReq(
        List<FullNotation> fnList,
        List<Long> boundary,
        Long strand,
        Long cid
) {}