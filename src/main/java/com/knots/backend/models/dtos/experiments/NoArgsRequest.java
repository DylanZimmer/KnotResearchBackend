
package com.knots.backend.models.dtos.experiments;

import com.knots.backend.models.dtos.MoveType;

public record NoArgsRequest(MoveType type) implements MoveRequest<Void, Void> {
    @Override
    public Void location() {
        return null;
    }
    @Override
    public Long numTimes() {
        return null;
    }
}
