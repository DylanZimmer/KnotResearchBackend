package com.knots.backend.models.entities;

import com.knots.backend.models.keys.DiagramKey;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class CurrentDiagram {
    @EmbeddedId
    private DiagramKey id;
    private String handedness;
}