package com.knots.backend.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class FullNotationRolf {
    @Id
    private Long strandId;
    private Long knotId;
    private String placement;
    private Long arcIn;
    private Long arcOut;
    private Long crossingId;
    private Long cidBefore;
    private Long cidAfter;
}
