package com.knots.backend.models.dtos;


public record InvariantsDto(
    String alexander_polynomial,
    Long determinant,
    Long writhe
) {}
