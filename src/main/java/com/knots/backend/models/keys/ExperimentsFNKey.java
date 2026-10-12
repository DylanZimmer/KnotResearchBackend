package com.knots.backend.models.keys;

import com.knots.backend.models.dtos.Placement;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ExperimentsFNKey implements Serializable {
    private Long experimentId;
    private Long ogKnotId;
    private Long stateNum;
    @Enumerated(EnumType.STRING)
    private Placement placement;
    private Long crossingId;
}