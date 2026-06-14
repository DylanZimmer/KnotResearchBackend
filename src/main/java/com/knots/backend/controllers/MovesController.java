package com.knots.backend.controllers;

import com.knots.backend.models.dtos.GeometryDto;
import com.knots.backend.models.dtos.GeometryRequests.AddTwistRequest;
import com.knots.backend.services.GeometryService;
import com.knots.backend.services.InvariantService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/add_twist")
    public void performAddTwist(@RequestBody AddTwistRequest request) {
        geometryService.performTwist(request.line(), request.handedness());
        invariantService.performTwist(request.line(), request.handedness());

    }

}
