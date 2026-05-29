package com.knots.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class CrossingSpecsRolf {
    @Id
    private Long csId;
    private Long diagramId;
    private Long crossingId;
    private Long underLine;
    private Long overLine;
    @Column(name="crossing_x")
    private Long crossingX;
    @Column(name="crossing_y")
    private Long crossingY;
}
