package com.knots.backend.models.entities;

import com.knots.backend.models.converters.InvariantsConverter;
import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.keys.ExperimentStatesKey;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    // Saved by name (e.g. "addTwist"), matching the varchar move_type column.
    @Enumerated(EnumType.STRING)
    private MoveType moveType;
    private String experimentStatus;
    @Convert(converter = InvariantsConverter.class)
    @Column(columnDefinition = "TEXT")
    private InvariantsDto invariants;
    // subStateNum of the state at stateNum - 1 that this one was made from (null for the start state).
    private Long parentSubStateNum;
    // Where the move was applied, e.g. "strand=3, sign=pos".
    private String moveDetail;

    public ExperimentStates(ExperimentStatesKey id, MoveType moveType, String experimentStatus, InvariantsDto invariants) {
        this(id, moveType, experimentStatus, invariants, null, null);
    }
}
