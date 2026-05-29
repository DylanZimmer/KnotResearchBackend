package com.knots.backend.models.keys;

import jakarta.persistence.Embeddable;

@Embeddable
public class DiagramKey {
    private Long diagramId;
    private Long extension;
}