package com.knots.backend.models.dtos.sage;

import com.knots.backend.models.dtos.LongPair;

public record AlexanderRequest(LongPair[][] matrix, Integer removeRow, Integer removeColumn) {

    public static AlexanderRequest usingLastRowAndColumn(LongPair[][] matrix) {
        return new AlexanderRequest(matrix, null, null);
    }
}
