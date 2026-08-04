package com.knots.backend.models.dtos;

import com.knots.backend.models.dtos.Direction;

public record DrawnLine (Long cid1, Direction dn1, Direction dn2, Long cid2, SameSeg sameSeg) {
    public enum SameSeg { Y }

    public DrawnLine(Long cid1, Direction dn1, Direction dn2, Long cid2) {
        this(cid1, dn1, dn2, cid2, null);
    }
}