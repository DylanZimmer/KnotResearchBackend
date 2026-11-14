package com.knots.backend.controllers;

import com.knots.backend.models.dtos.KnotDto;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.services.KnotService;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api/knots")
@RequiredArgsConstructor
public class KnotController {

    private final KnotService knotService;

    @GetMapping("/rolf_names")
    public List<KnotDto> getGroupedKnots() {
        return knotService.getGroupedKnots();
    }

    @GetMapping("/knot_id")
    public Long getKnotId( @RequestParam("num_crossings") String numCrossings, @RequestParam("rolf_index") String rolfIndex) {
        return knotService.getKnotId(numCrossings, rolfIndex);
    }

    // The knot's stored FullNotation (from full_notation_rolf), used to draw it and apply moves.
    @GetMapping("/full_notation")
    public List<FullNotation> getFullNotation(@RequestParam Long knotId) {
        return knotService.getFullNotation(knotId);
    }

    @GetMapping("/diagram_id")
    public Long getDiagramId( @RequestParam("num_crossings") String numCrossings, @RequestParam("rolf_index") String rolfIndex) {
        return knotService.getDiagramId(numCrossings, rolfIndex);
    }
}
