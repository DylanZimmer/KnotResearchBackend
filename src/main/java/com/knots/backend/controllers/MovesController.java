package com.knots.backend.controllers;

import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.services.FullNotationMovesService;
import com.knots.backend.services.InvariantCalculationsService;
import com.knots.backend.models.dtos.R3ableListReq;
import com.knots.backend.models.dtos.R3Req;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moves")
@RequiredArgsConstructor
public class MovesController {

    private final InvariantCalculationsService invariantCalculationsService;
    private final FullNotationMovesService fullNotationMovesService;

    @PostMapping("mirror")
    public List<FullNotation> mirror(@RequestBody List<FullNotation> fnList) {
        return fullNotationMovesService.mirror(fnList);
    }

    @PostMapping("/orientation_flip")
    public List<FullNotation> orientationFlip(@RequestBody List<FullNotation> fnList) {
        return fullNotationMovesService.orientationFlip(fnList);
    }

    @PostMapping("calculate_invariants")
    public void calculateInvariants(@RequestBody List<FullNotation> fnList) {
        invariantCalculationsService.getAllInvariants(fnList);
    }

    @GetMapping("/getBoundaries")
    public List<List<LongPair>> getBoundaries(@RequestBody List<FullNotation> fnList) {
        return fullNotationMovesService.getOrientedBoundaries(fnList);
    }

    @GetMapping("/r3ableList")
    public List<List<Long>> r3ableList(@RequestBody R3ableListReq listReq) {
        return fullNotationMovesService.r3ableList(listReq.fnList(), listReq.boundaries());
    }

    @GetMapping("/singleR3able")
    public boolean r3able(@RequestBody List<FullNotation> fnList, @RequestParam Long strand1, @RequestParam Long strand2, @RequestParam Long strand3) {
        return fullNotationMovesService.triangleR3able(fnList, List.of(strand1, strand2, strand3));
    }

    @PostMapping("/addTwist")
    public List<FullNotation> addTwist(@RequestBody List<FullNotation> fnList, Long strand, Sign sign) {
        return fullNotationMovesService.addTwist(fnList, strand, sign);
    }

    @PostMapping("/addR2")
    public List<FullNotation> addR2(@RequestBody List<FullNotation> fnList, @RequestParam LongPair underStrandOriented, @RequestParam LongPair overStrandOriented) {
        return fullNotationMovesService.addR2(fnList, underStrandOriented, overStrandOriented);
    }

    @PostMapping("/r3Slide")
    public List<FullNotation> r3Slide(@RequestBody R3Req r3Req) {
        return fullNotationMovesService.r3Slide(r3Req.fnList(), r3Req.boundary(), r3Req.strand(), r3Req.cid());
    }

    @PostMapping("/saddle")
    public List<FullNotation> saddle(@RequestBody List<FullNotation> fnList, @RequestParam Long strand1, @RequestParam Long strand2) {
        return fullNotationMovesService.saddle(fnList, strand1, strand2);
    }

}
