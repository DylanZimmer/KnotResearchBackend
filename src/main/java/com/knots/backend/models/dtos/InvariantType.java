package com.knots.backend.models.dtos;

public enum InvariantType {
    Alexander_Polynomial(String.class),
    Determinant(Long.class),
    Writhe(Long.class);

    private final Class<?> valueType;

    InvariantType(Class<?> valueType) {
        this.valueType = valueType;
    }

    public Class<?> getValueType() {
        return valueType;
    }
}