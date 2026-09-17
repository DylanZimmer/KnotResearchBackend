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
            newFnList.add(nextLine);
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

    //Get 2,3,... boundary crossings, then switch up strategy at the end to split
        //up the large one / check if there's only one
    /*
    public List<List<Long>> getPossibleR2Options(List<FullNotation> fnListInit) {
        List<FullNotation> fnList = fnListInit;
        List<List<Long>> compatibleStrands = new ArrayList<>();
        Long maxCid = 0L;
        for (Long cid = 0L; cid <= maxCid; cid++) {
            if (fnList.get(crossingId).equals(cid)) {

            }
        }
        return compatibleStrands;
    }

    //Do this by strands. Convert to strand before feeding to this
    public List<FullNotation> addR2(List<FullNotation> fnList, Long overStrand, Long underStrand) {
        List<FullNotation> newFnList = new ArrayList<>();
        for (FullNotation fn : fnList) {
            FullNotation nextLine = new FullNotation();
            nextLine.setKnotId(fn.getKnotId());
            if (overStrand.equals(fn.getStrandBefore())) {

            } else if (overStrand.equals(fn.getStrandAfter())) {

            } else if (underStrand.equals(fn.getStrandBefore())) {

            } else if (underStrand.equals(fn.getStrandAfter())) {

            } else {

            }
            newFnList.add(nextLine);
        }



        return newFnList;
    }
    */

}
