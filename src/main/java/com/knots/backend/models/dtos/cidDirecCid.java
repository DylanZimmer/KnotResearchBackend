package com.knots.backend.models.dtos;

public record cidDirecCid (Long cid1, Direction dn, Long cid2) {
    public enum Direction { U, D, R, L }
}