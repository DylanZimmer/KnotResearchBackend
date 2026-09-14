package com.knots.backend.models.keys;

import jakarta.persistence.Embeddable;
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
    private String placement;
    private Long crossingId;
}