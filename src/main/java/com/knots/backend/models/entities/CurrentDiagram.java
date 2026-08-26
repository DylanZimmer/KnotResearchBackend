package com.knots.backend.models.entities;

import com.knots.backend.models.keys.DiagramKey;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Setter;

@Setter
@Data
@Entity
public class CurrentDiagram {
    @Id
    Long DiagramId;
    private String handedness;
    private Long Extension;
}