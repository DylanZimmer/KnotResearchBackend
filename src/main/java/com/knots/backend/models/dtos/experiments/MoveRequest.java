package com.knots.backend.models.dtos.experiments;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.knots.backend.models.dtos.MoveType;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = AddTwistRequest.class,
                name = "addTwist"
        )
})
public sealed interface MoveRequest<L, C>
        permits AddTwistRequest {

    MoveType type();
    L location();
    C typeSpecificChoice();
    Long until();
}