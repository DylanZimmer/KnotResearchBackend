package com.knots.backend.models.entities;

import com.knots.backend.models.keys.ExperimentStatesKey;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.knots.backend.models.dtos.MoveType;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExperimentStates {
    @EmbeddedId
    private ExperimentStatesKey id;
    private MoveType moveType;
    private String experimentStatus;
    private String alexanderPolynomial;
    private Long determinant;
    private Long writhe;
}
