package com.knots.backend.controllers;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.services.GeometryService;
import com.knots.backend.services.InvariantCalculationsService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moves")
@RequiredArgsConstructor
public class MovesController {

    private final GeometryService geometryService;
    private final InvariantCalculationsService invariantCalculationsService;

    @PostMapping("populate_current")
    public void copyGeometryByDiagramId(@RequestParam Long knotId) {
        Long diagramId = geometryService.getDiagramIdByKnotId(knotId);
        geometryService.clearCurrentGeometry();
        geometryService.copyGeometryByDiagramId(diagramId);
    }

    @PostMapping("mirror")
    public void performMirror() {
        geometryService.performMirror();
    }

    @PostMapping("/orientation_flip")
    public void performOrientationFlip() {
        geometryService.performOrientationFlip();
    }

    @PostMapping("calculate_invariants")
    public void calculateInvariants(@RequestBody List<FullNotation> fn) { invariantCalculationsService.getAllInvariants(fn); }

    @GetMapping("/getBoundaries")
    public List<List<Pair<LongPair, LongPair>>> getBoundaries() {return geometryService.getBoundaries();}

}
