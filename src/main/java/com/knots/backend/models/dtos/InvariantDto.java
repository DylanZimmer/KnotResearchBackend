package com.knots.backend.models.dtos;

public record InvariantDto(
    Long determinant,
    String alexander_polynomial,
    String jones_polynomial
) {}
