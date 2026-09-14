package com.knots.backend.models.entities;

import com.knots.backend.models.keys.FullNotationKey;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class FullNotationRolf {
        @EmbeddedId
        private FullNotationKey id;
        private Long cidBefore;
        private Long cidAfter;
        private Long strandBefore;
        private Long strandAfter;
        private Long sign;
}