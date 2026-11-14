package com.knots.backend.models.dtos.experiments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.knots.backend.models.dtos.MoveType;

// visible = true also hands "type" to the record, which NoArgsRequest needs to know whether it is
// a mirror or a flip (otherwise its type is null and the request is rejected). The other records
// have no "type" field (they work their type out themselves), so unknown properties are ignored.
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoArgsRequest.class, name = "mirror"),
        @JsonSubTypes.Type(value = NoArgsRequest.class, name = "flip"),
        @JsonSubTypes.Type(value = AddTwistRequest.class, name = "addTwist"),
        @JsonSubTypes.Type(value = AddPokeRequest.class, name = "addPoke"),
        @JsonSubTypes.Type(value = SlideRequest.class, name = "slide"),
        @JsonSubTypes.Type(value = SaddleRequest.class, name = "saddle"),
        @JsonSubTypes.Type(value = RemTwistRequest.class, name = "remTwist"),
        @JsonSubTypes.Type(value = RemPokeRequest.class, name = "remPoke"),
        @JsonSubTypes.Type(value = NoArgsRequest.class, name = "remPokesAndTwists"),
        @JsonSubTypes.Type(value = SwitchPlacementRequest.class, name = "switchPlacement"),
})
public sealed interface MoveRequest<L, C>
        permits NoArgsRequest, AddTwistRequest, AddPokeRequest, SlideRequest, SaddleRequest, RemTwistRequest, RemPokeRequest, SwitchPlacementRequest {

    MoveType type();
    L location();
    default C typeSpecificChoice() {
        return null;
    };
    Long numTimes();
}