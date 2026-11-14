package com.knots.backend.models.dtos.experiments;

// Optional filters for which slideable triangles a slide request may use. Either can be null.
//   crossing: at least one of the triangle's three crossings must match (sign / placement)
//   strand:   at least one of the triangle's three strands must match
public record SlideLoc(
        CrossingTypeDto crossing,
        StrandTypeDto strand
) {}
