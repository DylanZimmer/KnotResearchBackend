package com.knots.backend.models.dtos;

public record DrawnLine (Long cid1, Direction dn1, Direction dn2, Long cid2, SameSeg sameSeg) {
    public enum Direction { U, D, R, L }
    public enum SameSeg { Y }

    public DrawnLine(Long cid1, Direction dn1, Direction dn2, Long cid2) {
        this(cid1, dn1, dn2, cid2, null);
    }
}