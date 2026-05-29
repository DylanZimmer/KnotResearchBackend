package com.knots.backend.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DiagramsRolf {
    @Id
    private Long knotId;
    private String nameRolf;
    private String conversionForFullNotation;
    private String startLine;
    private Long diagramId;
}
