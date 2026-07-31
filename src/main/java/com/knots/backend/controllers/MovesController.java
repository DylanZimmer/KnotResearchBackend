package com.knots.backend.controllers;

import com.knots.backend.models.dtos.DrawnLine;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.MoveRequests.AddTwistRequest;
import com.knots.backend.models.dtos.MoveRequests.AddPokeRequest;
import com.knots.backend.models.dtos.Walk;
import com.knots.backend.services.GeometryService;
import com.knots.backend.services.InvariantService;
import com.knots.backend.models.dtos.GeometricLine;

import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moves")
@RequiredArgsConstructor
public class MovesController {

    private final GeometryService geometryService;
    private final InvariantService invariantService;

    @PostMapping("populate_current")
    public void copyGeometryByDiagramId(@RequestParam Long knotId) {
        Long diagramId = geometryService.getDiagramIdByKnotId(knotId);
        geometryService.clearCurrentGeometry();
        geometryService.copyGeometryByDiagramId(diagramId);
        invariantService.clearCurrentFullNotation();
        invariantService.copyFullNotationByKnotId(knotId);
    }

    @PostMapping("mirror")
    public void performMirror() {
        geometryService.performMirror();
        invariantService.performMirror();
    }

    @PostMapping("/orientation_flip")
    public void performOrientationFlip() {
        geometryService.performOrientationFlip();
        invariantService.performOrientationFlip();
    }

    /*
    @PostMapping("/add_twist")
    public void performAddTwist(@RequestBody AddTwistRequest request) {
        geometryService.performTwist(request.line(), request.handedness());
        invariantService.performTwist(request.line(), request.handedness());
    }

    @PostMapping("/add_poke")
    public void performAddPoke(@RequestBody AddPokeRequest request) {
        geometryService.performPoke(request.line1(), request.line2());
        //invariantService.performPoke(request.line1(), request.line2());
    }

    @GetMapping("/all_possible_pokes")
    public Map<GeometricLine, List<GeometricLine>> getAllPokeOptions() {
        return invariantService.getAllPokeOptions();
    }
     */

    @GetMapping("/getSegs")
    public List<Pair<LongPair, LongPair>> getBoundaries() {return geometryService.getSegments();}




}
