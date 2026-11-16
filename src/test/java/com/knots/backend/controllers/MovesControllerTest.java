package com.knots.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knots.backend.config.StringToLongPairConverter;
import com.knots.backend.models.dtos.Placement;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.services.FullNotationMovesService;
import com.knots.backend.services.InvariantCalculationsService;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovesControllerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final MockMvc mvc;

    MovesControllerTest() {
        FormattingConversionService conversions = new FormattingConversionService();
        conversions.addConverter(new StringToLongPairConverter());
        mvc = MockMvcBuilders.standaloneSetup(new MovesController(mock(InvariantCalculationsService.class), new FullNotationMovesService()))
                .setControllerAdvice(new com.knots.backend.config.BadMoveHandler())
                .setConversionService(conversions).build();
    }

    private String loop() throws Exception {
        return json.writeValueAsString(List.of(
                new FullNotation(1L, 1L, Placement.over, 1L, 1L, 0L, 1L, Sign.pos),
                new FullNotation(1L, 1L, Placement.under, 1L, 1L, 1L, 0L, Sign.pos)));
    }

    @Test
    void pokeReadsLongPairQueryParameters() throws Exception {
        mvc.perform(post("/api/moves/addPoke").param("underStrandOriented", "0,1").param("overStrandOriented", "1,1")
                        .contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void switchPlacementChangesTheCrossing() throws Exception {
        mvc.perform(post("/api/moves/switch_placement").param("cid", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placement").value("under"))
                .andExpect(jsonPath("$[0].sign").value("neg"))
                .andExpect(jsonPath("$[1].placement").value("over"));
    }

    @Test
    void removalsListAndUndoTheLoopTwist() throws Exception {
        mvc.perform(post("/api/moves/removable").contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twists[0]").value(1))
                .andExpect(jsonPath("$.pokes.length()").value(0));
        mvc.perform(post("/api/moves/remTwist").param("cid", "1").contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/moves/remPokesAndTwists").contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void impossibleRemovalsAreBadRequests() throws Exception {
        mvc.perform(post("/api/moves/remPoke").param("cid1", "1").param("cid2", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Crossings 1 and 2 are not a poke"));
    }

    @Test
    void twistTakesStrandAndSign() throws Exception {
        mvc.perform(post("/api/moves/addTwist").param("strand", "0").param("sign", "neg")
                        .contentType(MediaType.APPLICATION_JSON).content(loop()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }
}
