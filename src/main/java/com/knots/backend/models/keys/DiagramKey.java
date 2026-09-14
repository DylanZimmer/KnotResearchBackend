package com.knots.backend.models.keys;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Data;

@Data
@Embeddable
public class DiagramKey implements Serializable {
    private Long diagramId;
    private Long extension;
}