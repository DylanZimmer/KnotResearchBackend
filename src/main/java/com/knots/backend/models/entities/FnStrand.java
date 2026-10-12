package com.knots.backend.models.entities;

import com.knots.backend.models.dtos.Placement;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FnStrand {
    private Long cid;
    private Placement placement;
    private String into_outOf;
}
