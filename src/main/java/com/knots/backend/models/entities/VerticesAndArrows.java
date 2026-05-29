package com.knots.backend.models.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class VerticesAndArrows {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vaId;
    private Long diagramId;
    private Long extension;
    private Long startPoint;
    private Long endPoint;
    @Column(name="strand_x")
    private Long strandX;
    @Column(name="strand_y")
    private Long strandY;
}
