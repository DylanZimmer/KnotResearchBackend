package com.knots.backend.services;

import com.knots.backend.models.dtos.GeometricLine;
import com.knots.backend.models.dtos.HalfGeometricLine;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FullNotationRolf;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.repositories.FullNotationRolfRepo;
import com.knots.backend.repositories.FullNotationRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            copy.setCidBefore(fn.getCidBefore());
            copy.setCidAfter(fn.getCidAfter());
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
            Long tempArcIn = fn.getArcIn();
            fn.setArcIn(fn.getArcOut());
            fn.setArcOut(tempArcIn);
            Long tempCidBefore = fn.getCidBefore();
            fn.setCidBefore(fn.getCidAfter());
            fn.setCidAfter(tempCidBefore);
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
        //Need to put in the cid_before and cid_after
        fullNotationRepo.save(twist);
    }

    private List<GeometricLine> createLinesFromArcs(List<LongPair> oppArcs) {
        List<GeometricLine> lines = new ArrayList<>();
        Long maxArc = fullNotationRepo.getMaxArc();
        for (LongPair arcs : oppArcs) {
            HalfGeometricLine halfLineFromIn;
            if (arcs.x() == 0 || arcs.x().equals(maxArc)) {
                halfLineFromIn = fullNotationRepo.getLineFrom0OrMaxArcIn(arcs.x(), maxArc);
            } else {
                halfLineFromIn = fullNotationRepo.getLineFromArcIn(arcs.x());
            }
            HalfGeometricLine halfLineOfIn = fullNotationRepo.getLineOfArcIn(arcs.x());
            lines.add(new GeometricLine(halfLineOfIn.cid(), halfLineOfIn.placement(), halfLineFromIn.cid(), halfLineFromIn.placement()));
            HalfGeometricLine halfLineFromOut;
            if (arcs.y() == 0 || arcs.y().equals(maxArc)) {
                halfLineFromOut = fullNotationRepo.getLineFrom0OrMaxArcOut(arcs.y(), maxArc);
            } else {
                halfLineFromOut = fullNotationRepo.getLineFromArcOut(arcs.y());
            }
            HalfGeometricLine halfLineOfOut = fullNotationRepo.getLineOfArcOut(arcs.y());
            lines.add(new GeometricLine(halfLineOfOut.cid(), halfLineOfOut.placement(), halfLineFromOut.cid(), halfLineFromOut.placement()));
        }
        return lines;
    }

    public List<GeometricLine> createPokeList(GeometricLine line) {
        List<GeometricLine> pokeList = new ArrayList<>();
        List<LongPair> oppArcs1 = fullNotationRepo.getArcsOfCrossingPairs(line.cid1(), line.placement1());
        List<LongPair> oppArcs2 = fullNotationRepo.getArcsOfCrossingPairs(line.cid2(), line.placement2());
        pokeList.addAll(createLinesFromArcs(oppArcs1));
        pokeList.addAll(createLinesFromArcs(oppArcs2));
        return pokeList;
    }

    //This is very very wrong, look at 8_2 for breaks on both the inner and outer knot on katlas.org
    public Map<GeometricLine, List<GeometricLine>> getAllPokeOptions() {
        List<FullNotation> fnList = fullNotationRepo.findAll();
        //Doesn't account for twists and previous pokes, which will break o/u logic
        Map<GeometricLine, List<GeometricLine>> possiblePokes = new HashMap<>();
        Long maxArc = fullNotationRepo.getMaxArc();
        for (FullNotation fn : fnList) {
            //Only get the line from arc_in, each line has one of each and this avoids duplicates
            HalfGeometricLine halfLine;
            if (fn.getArcIn() == 0 || fn.getArcIn().equals(maxArc)) {
                halfLine = fullNotationRepo.getLineFrom0OrMaxArcIn(fn.getArcIn(), maxArc);
            } else {
                halfLine = fullNotationRepo.getLineFromArcIn(fn.getArcIn());
            }
            GeometricLine line = new GeometricLine(fn.getCrossingId(), fn.getPlacement(), halfLine.cid(), halfLine.placement());
            List<GeometricLine> pokes = createPokeList(line);
            possiblePokes.put(line, pokes);
        }
        return possiblePokes;
    }

    @Transactional
    public void performPoke(GeometricLine line1, GeometricLine line2, String placement) {
        /*
        LongPair segment1 = fullNotationRepo.getHalfLineAsArcs(line1.cid1(), line1.placement1)
        LongPair arcs11 = fullNotationRepo.getArcs(line1.cid1(), line1.placement1());
        LongPair arcs12 = fullNotationRepo.getArcs(line1.cid2(), line1.placement2());
        LongPair arcs21 = fullNotationRepo.getArcs(line2.cid1(), line2.placement1());
        LongPair arcs22 = fullNotationRepo.getArcs(line2.cid2(), line2.placement2());
        /*
        This is each arc from each row, where really I just need the two matching sets

        line1 will be poked 'placement' through line2

        Need to get the minimum of all relevant arcs and nudge up from there
        Then I need 3
        */

    }

}