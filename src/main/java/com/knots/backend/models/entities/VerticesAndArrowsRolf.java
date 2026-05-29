package com.knots.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class VerticesAndArrowsRolf {
    @Id
    private Long vaId;
    private Long diagramId;
    private Long startPoint;
    private Long endPoint;
    @Column(name="strand_x")
    private Long strandX;
    @Column(name="strand_y")
    private Long strandY;

}
