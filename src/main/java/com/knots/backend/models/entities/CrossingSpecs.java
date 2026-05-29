package com.knots.backend.models.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class CrossingSpecs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long csId;
    private Long diagramId;
    private Long extension;
    private Long crossingId;
    private Long underLine;
    private Long overLine;
    @Column(name = "crossing_x")
    private Long crossingX;
    @Column(name = "crossing_y")
    private Long crossingY;
}