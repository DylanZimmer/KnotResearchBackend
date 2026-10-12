package com.knots.backend.models.entities;

import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.Placement;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FullNotation {
    private Long knotId;
    private Long crossingId;
    private Placement placement;
    private Long cidBefore;
    private Long cidAfter;
    private Long strandBefore;
    private Long strandAfter;
    private Sign sign;
}