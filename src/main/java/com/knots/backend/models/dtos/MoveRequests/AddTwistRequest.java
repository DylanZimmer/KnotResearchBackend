package com.knots.backend.models.dtos.MoveRequests;

import com.knots.backend.models.dtos.GeometricLine;

public record AddTwistRequest (GeometricLine line, String handedness) {};