package com.knots.backend.controllers;

import com.knots.backend.models.dtos.*;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.services.FullNotationMovesService;
import com.knots.backend.services.InvariantCalculationsService;

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
    public InvariantsDto calculateInvariants(@RequestBody List<FullNotation> fnList) {
        return invariantCalculationsService.getAllInvariants(fnList);
    }

    @PostMapping("/get_oriented_boundaries")
    public List<List<LongPair>> getOrientedBoundaries(@RequestBody List<FullNotation> fnList) {
        return fullNotationMovesService.getOrientedBoundaries(fnList);
    }

    @PostMapping("/slideableList")
    public List<List<Long>> slideableList(@RequestBody SlideableListReq listReq) {
        return fullNotationMovesService.slideableList(listReq.fnList(), listReq.boundaries());
    }

    @GetMapping("/singleslideable")
    public boolean slideable(@RequestBody List<FullNotation> fnList, @RequestParam Long strand1, @RequestParam Long strand2, @RequestParam Long strand3) {
        return fullNotationMovesService.triangleBdySlideable(fnList, List.of(strand1, strand2, strand3));
    }

    @PostMapping("/addTwist")
    public List<FullNotation> addTwist(@RequestBody List<FullNotation> fnList, @RequestParam Long strand, @RequestParam Sign sign) {
        return fullNotationMovesService.addTwist(fnList, strand, sign);
    }

    @PostMapping("/addPoke")
    public List<FullNotation> addPoke(@RequestBody List<FullNotation> fnList, @RequestParam LongPair underStrandOriented, @RequestParam LongPair overStrandOriented) {
        return fullNotationMovesService.addPoke(fnList, underStrandOriented, overStrandOriented);
    }

    @PostMapping("/Slide")
    public List<FullNotation> Slide(@RequestBody SlideReq Req) {
        if (Req.boundary() != null && Req.boundary().size() == 3) {
            return fullNotationMovesService.slideTriangle(Req.fnList(), Req.boundary());
        }
        return fullNotationMovesService.Slide(Req.fnList(), Req.strand(), Req.cid());
    }

    // Crossing change: swaps over / under at crossing cid.
    @PostMapping("/switch_placement")
    public List<FullNotation> switchPlacement(@RequestBody List<FullNotation> fnList, @RequestParam Long cid) {
        return fullNotationMovesService.switchPlacement(fnList, cid);
    }

    // Twists and pokes that can be removed from this diagram.
    @PostMapping("/removable")
    public RemovableDto removable(@RequestBody List<FullNotation> fnList) {
        return new RemovableDto(fullNotationMovesService.getAllTwisted(fnList), fullNotationMovesService.getAllPoked(fnList));
    }

    @PostMapping("/remTwist")
    public List<FullNotation> remTwist(@RequestBody List<FullNotation> fnList, @RequestParam Long cid) {
        return fullNotationMovesService.remTwist(fnList, cid);
    }

    @PostMapping("/remPoke")
    public List<FullNotation> remPoke(@RequestBody List<FullNotation> fnList, @RequestParam Long cid1, @RequestParam Long cid2) {
        return fullNotationMovesService.remPoke(fnList, cid1, cid2);
    }

    @PostMapping("/remPokesAndTwists")
    public List<FullNotation> remPokesAndTwists(@RequestBody List<FullNotation> fnList) {
        return fullNotationMovesService.remPokesAndTwists(fnList);
    }

    @PostMapping("/saddle")
    public List<FullNotation> saddle(@RequestBody List<FullNotation> fnList, @RequestParam Long strand1, @RequestParam Long strand2) {
        return fullNotationMovesService.saddle(fnList, strand1, strand2);
    }

}
