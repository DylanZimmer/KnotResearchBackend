package com.knots.backend.models.dtos.MoveRequests;

import com.knots.backend.models.dtos.GeometricLine;

public record AddPokeRequest (GeometricLine line1, GeometricLine line2) {};