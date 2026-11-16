package com.knots.backend.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knots.backend.models.dtos.MoveType;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.experiments.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// The JSON the frontend sends for each move type (services/experiment.ts toMoveRequests).
class MoveRequestJsonTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void everyMoveTypeKeepsItsType() throws Exception {
        List<MoveRequest<?, ?>> requests = json.readValue("""
                [
                  {"type": "mirror"},
                  {"type": "flip"},
                  {"type": "addTwist", "location": {"crossing_before": null, "crossing_after": null, "orientedWith": null}, "typeSpecificChoice": "neg", "numTimes": 2},
                  {"type": "addPoke", "location": {"strands": null, "crossings": null, "numCrossings": 2}, "numTimes": 1},
                  {"type": "slide", "location": {"crossing": {"sign": null, "placement": null}, "strand": null}, "numTimes": 0},
                  {"type": "saddle", "location": {"strands": null, "crossings": null, "numCrossings": null}, "numTimes": 3},
                  {"type": "remTwist", "location": {"sign": "pos", "placement": null}, "numTimes": 1},
                  {"type": "remPoke", "numTimes": 0},
                  {"type": "remPokesAndTwists"},
                  {"type": "switchPlacement", "location": {"sign": "neg", "placement": null}, "numTimes": 2}
                ]
                """, new TypeReference<>() {});
        assertThat(requests).extracting(MoveRequest::type).containsExactly(
                MoveType.mirror, MoveType.flip, MoveType.addTwist, MoveType.addPoke, MoveType.slide, MoveType.saddle,
                MoveType.remTwist, MoveType.remPoke, MoveType.remPokesAndTwists, MoveType.switchPlacement);
        assertThat(((SwitchPlacementRequest) requests.get(9)).location().sign()).isEqualTo(Sign.neg);
        assertThat(requests.get(9).numTimes()).isEqualTo(2L);
        assertThat(((RemTwistRequest) requests.get(6)).location().sign()).isEqualTo(Sign.pos);
        assertThat(requests.get(7)).isInstanceOf(RemPokeRequest.class);
        assertThat(requests.get(7).numTimes()).isEqualTo(0L);
        assertThat(requests.get(8)).isInstanceOf(NoArgsRequest.class);
        assertThat(requests.get(0)).isInstanceOf(NoArgsRequest.class);
        assertThat(((AddTwistRequest) requests.get(2)).typeSpecificChoice()).isEqualTo(Sign.neg);
        assertThat(requests.get(2).numTimes()).isEqualTo(2L);
        assertThat(((SlideRequest) requests.get(4)).location().crossing()).isNotNull();
        assertThat(requests.get(5).numTimes()).isEqualTo(3L);
    }
}
