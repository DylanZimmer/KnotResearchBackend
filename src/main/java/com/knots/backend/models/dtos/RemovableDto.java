package com.knots.backend.models.dtos;

import java.util.List;

// What can be removed from a diagram right now: twist crossings, and pairs of crossings forming a poke.
public record RemovableDto(List<Long> twists, List<LongPair> pokes) {}
