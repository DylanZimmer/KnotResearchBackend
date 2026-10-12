package com.knots.backend.models.dtos;

public enum Sign {
    pos(1L),
    neg(-1L);

    private final long value;

    Sign(long value) { this.value = value; }

    public long value() { return value; }

    public Sign opposite() { return this == pos ? neg : pos; }

    public static Sign fromValue(long value) {
        if (value == 1L) return pos;
        if (value == -1L) return neg;
        throw new IllegalArgumentException("Invalid sign: " + value);
    }
}
