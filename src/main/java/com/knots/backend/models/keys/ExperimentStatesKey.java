package com.knots.backend.models.keys;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.io.Serializable;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Embeddable
    public class ExperimentStatesKey implements Serializable {
        private Long experimentId;
        private Long ogKnotId;
        private Long stateNum;
    }