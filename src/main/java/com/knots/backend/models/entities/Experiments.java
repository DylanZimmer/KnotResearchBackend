package com.knots.backend.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Id;

@Entity
@Data
@Table(name = "experiments")
public class Experiments {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long experimentId;
    private String experimentStatus;
}