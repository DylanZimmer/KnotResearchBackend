package com.knots.backend.models.entities;

import com.knots.backend.models.keys.ExperimentsFNKey;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "experiments_full_notation")
public class ExperimentsFullNotation {
    @EmbeddedId
    private ExperimentsFNKey id;
    private Long cidBefore;
    private Long cidAfter;
    private Long strandBefore;
    private Long strandAfter;
    private Long sign;
}