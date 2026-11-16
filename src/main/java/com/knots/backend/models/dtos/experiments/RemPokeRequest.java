package com.knots.backend.models.dtos.experiments;
import com.knots.backend.models.dtos.MoveType;

// Undo pokes (Reidemeister II). There is no location: every poke has one positive and one negative
// crossing, so a crossing filter wouldn't narrow anything down.
public record RemPokeRequest(Long numTimes) implements MoveRequest<Void, Void> {
    @Override
    public MoveType type() {
        return MoveType.remPoke;
    }

    @Override
    public Void location() {
        return null;
    }
}
