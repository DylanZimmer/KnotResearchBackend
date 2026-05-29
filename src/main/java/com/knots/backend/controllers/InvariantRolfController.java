package com.knots.backend.controllers;


import com.knots.backend.models.dtos.InvariantDto;
import com.knots.backend.services.InvariantRolfService;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/knots")
@RequiredArgsConstructor
public class InvariantRolfController {

    private final InvariantRolfService invariantRolfService;

    @GetMapping("/rolf_invariants")
    public InvariantDto getInvariantsByKnotId(@RequestParam Long knotId) {
        return invariantRolfService.getInvariantsByKnotId(knotId);
    }
}
