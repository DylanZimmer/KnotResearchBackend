package com.knots.backend.services;


import com.knots.backend.models.entities.FullNotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Service;

//Meant to take in full notation, perform move, return changed full notation
@Service
public class FullNotationMovesService {

    public List<FullNotation> mirror(List<FullNotation> fnList) {
        List<FullNotation> newFnList = new ArrayList<>();
        for (FullNotation fn : fnList) {
            String newPlacement;
            if (fn.getPlacement().equals("under")) {
                newPlacement = "over";
            } else {
                newPlacement = "under";
            }
            FullNotation newFn = new FullNotation(
                    fn.getKnotId(),
                    fn.getCrossingId(),
                    newPlacement,
                    fn.getCidBefore(),
                    fn.getCidAfter(),
                    fn.getStrandBefore(),
                    fn.getStrandAfter(),
                    (fn.getSign() * -1)
            );
            newFnList.add(newFn);
        }
        return newFnList;
    }

    public List<FullNotation> orientationFlip(List<FullNotation> fnList) {
        List<FullNotation> newFnList = new ArrayList<>();
        for (FullNotation fn : fnList) {
            FullNotation newFn = new FullNotation(
                    fn.getKnotId(),
                    fn.getCrossingId(),
                    fn.getPlacement(),
                    fn.getCidAfter(),
                    fn.getCidBefore(),
                    fn.getStrandAfter(),
                    fn.getStrandBefore(),
                    fn.getSign()
            );
            newFnList.add(newFn);
        }
        return newFnList;
    }

    private Long setStrand(Long twistStrand, Long currStrand) {
        if (currStrand >= twistStrand) {
            return currStrand + 2;
        } else {
            return currStrand;
        }
    }

    public List<FullNotation> addTwist(List<FullNotation> fnList, Long strand, Long sign) {
        List<FullNotation> newFnList = new ArrayList<>();
        //The strand n should be split up into n and n+1.
            //Then every subsequent strand should be itself + 1
                    //I NEED TWO NEW LINES, one for each placement
        //They'll share a strand, that strand is the loop
                //The newLine should be same knotId, crossingId = max of cid with strand,
        //with every subsequent cid as itself + 1,
        //Is that even better? I can just make it the max cid, the next one
        //Long twistCid = max(fnLineIntoTwist.getCrossingId(), fnLineOutOfTwist.getCrossingId());
        long twistCid = fnList.stream().mapToLong(FullNotation::getCrossingId).max().orElse(0L) + 1;
        long kId = fnList.get(0).getKnotId();
        long cidBefore = -1;
        long cidAfter = -1;
        for (FullNotation fn : fnList) {
            FullNotation nextLine = new FullNotation();
            nextLine.setKnotId(fn.getKnotId());
            nextLine.setCrossingId(fn.getCrossingId());
            nextLine.setPlacement(fn.getPlacement());
            if (fn.getStrandBefore().equals(strand)) {
                nextLine.setStrandBefore(setStrand(strand, strand));
                nextLine.setCidBefore(twistCid);
                nextLine.setCidAfter(fn.getCidAfter());
                cidBefore = fn.getCrossingId();
            } else if (fn.getStrandAfter().equals(strand)) {
                nextLine.setCidAfter(twistCid);
                nextLine.setCidBefore(fn.getCidBefore());
                nextLine.setStrandBefore(fn.getStrandBefore());
                cidAfter = fn.getCrossingId();
            } else {
                nextLine.setCidBefore(fn.getCidBefore());
                nextLine.setCidAfter(fn.getCidAfter());
                nextLine.setStrandBefore(setStrand(strand, fn.getStrandBefore()));
            }
            nextLine.setStrandAfter(setStrand(strand, fn.getStrandAfter()));
            nextLine.setSign(fn.getSign());
        }
        if (sign == 1) {
            newFnList.add(new FullNotation(kId, twistCid, "over", cidBefore, twistCid, strand, strand+1, sign));
            newFnList.add(new FullNotation(kId, twistCid, "under", twistCid, cidAfter, strand+1, strand+2, sign));
        } else if (sign == -1) {
            newFnList.add(new FullNotation(kId, twistCid, "under", cidBefore, twistCid, strand, strand+1, sign));
            newFnList.add(new FullNotation(kId, twistCid, "over", twistCid, cidAfter, strand+1, strand+2, sign));
        }
        return newFnList;
    }
}
