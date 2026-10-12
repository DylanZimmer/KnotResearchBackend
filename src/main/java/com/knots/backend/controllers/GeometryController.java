
package com.knots.backend.controllers;

import com.knots.backend.models.dtos.GeometryDto;
import com.knots.backend.services.GeometryService;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/geom")
@RequiredArgsConstructor
public class GeometryController {

    private final GeometryService geometryService;

    @PostMapping("switch_display")
    public void copyGeometryByDiagramId(@RequestParam Long knotId) {
        Long diagramId = geometryService.getDiagramIdByKnotId(knotId);
        geometryService.clearCurrentGeometry();
        geometryService.copyGeometryByDiagramId(diagramId);
    }

    @GetMapping("/diagram_info")
    public GeometryDto getGeometryByDiagramId() {
        return geometryService.getCurrentGeometry();
    }

    @GetMapping("/diagram_info_rolf")
    public GeometryDto getGeometryByDiagramId(@RequestParam Long diagramId) {
        return geometryService.getGeometryByDiagramId(diagramId);
    }
}
