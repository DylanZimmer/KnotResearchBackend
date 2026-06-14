package com.knots.backend.services;

import com.knots.backend.models.dtos.GeometricLine;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FullNotationRolf;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.repositories.FullNotationRolfRepo;
import com.knots.backend.repositories.FullNotationRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvariantService {

    private final FullNotationRolfRepo fullNotationRolfRepo;
    private final FullNotationRepo fullNotationRepo;

    @Transactional
    public void clearCurrentFullNotation() {
        fullNotationRepo.deleteAll();
    }

    @Transactional
    public void copyFullNotationByKnotId(long knotId) {
        List<FullNotationRolf> fnr = fullNotationRolfRepo.findByKnotId(knotId);
        List<FullNotation> fn_copy = new ArrayList<>();
        for (FullNotationRolf fn : fnr) {
            FullNotation copy = new FullNotation();
            copy.setStrandId(fn.getStrandId());
            copy.setKnotId(fn.getKnotId());
            copy.setPlacement(fn.getPlacement());
            copy.setArcIn(fn.getArcIn());
            copy.setArcOut(fn.getArcOut());
            copy.setCrossingId(fn.getCrossingId());
            fn_copy.add(copy);
        }
        fullNotationRepo.saveAll(fn_copy);
    }

    @Transactional
    public void performMirror() {
        List<FullNotation> fnList = fullNotationRepo.findAll();
        for (FullNotation fn : fnList) {
            String placement = fn.getPlacement();
            if (placement.equals("over")) {
                fn.setPlacement("under");
            }
            if (placement.equals("under")) {
                fn.setPlacement("over");
            }
        }
    }

    @Transactional
    public void performOrientationFlip() {
        List<FullNotation> fnList = fullNotationRepo.findAll();
        for (FullNotation fn : fnList) {
            Long temp = fn.getArcIn();
            fn.setArcIn(fn.getArcOut());
            fn.setArcOut(temp);
        }
    }

    @Transactional
    public void performTwist(GeometricLine line, String handedness) {
        LongPair arcs1 = fullNotationRepo.getArcs(line.cid1(), line.placement1());
        LongPair arcs2 = fullNotationRepo.getArcs(line.cid2(), line.placement2());
        LongPair newArcs;
        if (Math.abs(arcs1.x() - arcs2.y()) == 1) {
            newArcs = new LongPair(arcs2.y(), arcs1.x());
        } else if (Math.abs(arcs1.y() - arcs2.x()) == 1) {
            newArcs = new LongPair(arcs1.y(), arcs2.x());
        } else {
            throw new IllegalArgumentException("Selected lines are not adjacent");
        }
        FullNotation twist = new FullNotation();
        long currKnotId = fullNotationRepo.getCurrKnotId();
        twist.setKnotId(currKnotId);
        long maxStrandId = fullNotationRepo.getMaxStrandId();
        twist.setStrandId(maxStrandId + 1);
        twist.setPlacement(handedness + "-twist");
        twist.setArcIn(newArcs.x());
        twist.setArcOut(newArcs.y());
        fullNotationRepo.save(twist);
    }
}