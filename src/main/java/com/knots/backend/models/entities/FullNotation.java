package com.knots.backend.models.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class FullNotation {
    @Id
    private Long strandId;
    private Long knotId;
    private String placement;
    private Long arcIn;
    private Long arcOut;
    private Long crossingId;
}
