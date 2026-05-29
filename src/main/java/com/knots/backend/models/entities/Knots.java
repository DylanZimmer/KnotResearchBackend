package com.knots.backend.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Knots {
    @Id
    private Long knotId;
    private String numCrossings;
    private String rolfIndex;
}