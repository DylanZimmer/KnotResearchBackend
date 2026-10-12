package com.knots.backend.services;

import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.Placement;


import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FnStrand;
import com.knots.backend.models.entities.FullNotation;

import java.util.*;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

//Meant to take in full notation, perform move, return changed full notation
@Service
public class FullNotationMovesService {

    public List<FullNotation> mirror(List<FullNotation> fnList) {
        List<FullNotation> newFnList = new ArrayList<>();
        for (FullNotation fn : fnList) {
            Placement newPlacement;
            if (fn.getPlacement() == Placement.under) {
                newPlacement = Placement.over;
            } else {
                newPlacement = Placement.under;
            }
            FullNotation newFn = new FullNotation(
                    fn.getKnotId(),
                    fn.getCrossingId(),
                    newPlacement,
                    fn.getCidBefore(),
                    fn.getCidAfter(),
                    fn.getStrandBefore(),
                    fn.getStrandAfter(),
                    fn.getSign().opposite()
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

    private Long r1SetStrand(Long twistStrand, Long currStrand) {
        if (currStrand >= twistStrand) {
            return currStrand + 2;
        } else {
            return currStrand;
        }
    }

    public List<FullNotation> addTwist(List<FullNotation> fnList, Long strand, Sign sign) {
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
                nextLine.setStrandBefore(r1SetStrand(strand, strand));
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
                nextLine.setStrandBefore(r1SetStrand(strand, fn.getStrandBefore()));
            }
            nextLine.setStrandAfter(r1SetStrand(strand, fn.getStrandAfter()));
            nextLine.setSign(fn.getSign());
            newFnList.add(nextLine);
        }
        if (sign == Sign.pos) {
            newFnList.add(new FullNotation(kId, twistCid, Placement.over, cidBefore, twistCid, strand, strand+1, sign));
            newFnList.add(new FullNotation(kId, twistCid, Placement.under, twistCid, cidAfter, strand+1, strand+2, sign));
        } else if (sign == Sign.neg) {
            newFnList.add(new FullNotation(kId, twistCid, Placement.under, cidBefore, twistCid, strand, strand+1, sign));
            newFnList.add(new FullNotation(kId, twistCid, Placement.over, twistCid, cidAfter, strand+1, strand+2, sign));
        }
        return newFnList;
    }

    private Long r2SetStrand(Long strand, Long strand1, Long strand2) {
        if (strand > strand1) {
            if (strand > strand2) {
                return strand + 4;
            } else {
                return strand + 2;
            }
        } else {
            return strand;
        }
    }

    public List<FullNotation> addR2(List<FullNotation> fnList, LongPair underStrandOriented, LongPair overStrandOriented) {
        Long maxCid = fnList.stream().map(FullNotation::getCrossingId).max(Long::compareTo).orElse(0L);
        Long newCid1 = maxCid + 1;
        Long newCid2 = maxCid + 2;
        Long overStrand = overStrandOriented.x();
        Long underStrand = underStrandOriented.x();
        Long smallerStrand = Math.min(overStrand, underStrand);
        Long largerStrand = Math.max(overStrand, underStrand);
        List<FullNotation> newFnList = new ArrayList<>();
        for (FullNotation fn : fnList) {
            Long strandBefore = null;
            Long cidBefore = null;
            Long cidAfter = null;
            if (fn.getStrandBefore().equals(overStrand)) {
                cidBefore = newCid2;
                strandBefore = overStrand + 2;
                newFnList.add(new FullNotation(fn.getKnotId(), newCid1, Placement.over, fn.getCidBefore(), newCid2, overStrand, overStrand+1, Sign.pos));
            }
            if (fn.getStrandAfter().equals(overStrand)) {
                cidAfter = newCid1;
                newFnList.add(new FullNotation(fn.getKnotId(), newCid2, Placement.over, newCid1, fn.getCidAfter(), overStrand+1, overStrand+2, Sign.neg));
            }
            if (fn.getStrandBefore().equals(underStrand)) {
                strandBefore = underStrand + 2;
                if (overStrandOriented.y().equals(underStrandOriented.y())) {
                    cidBefore = newCid2;
                    newFnList.add(new FullNotation(fn.getKnotId(), newCid2, Placement.under, newCid1, fn.getCidAfter(), underStrand+1, underStrand+2, Sign.neg));
                } else {
                    cidBefore = newCid1;
                    newFnList.add(new FullNotation(fn.getKnotId(), newCid1, Placement.under, newCid2, fn.getCidAfter(), underStrand+1, underStrand+2, Sign.pos));
                }
            }
            if (fn.getStrandAfter().equals(underStrand)) {
                if (overStrandOriented.y().equals(underStrandOriented.y())) {
                    cidAfter = newCid1;
                    newFnList.add(new FullNotation(fn.getKnotId(), newCid1, Placement.under, fn.getCidBefore(), newCid2, underStrand, underStrand + 1, Sign.pos));
                } else {
                    cidAfter = newCid2;
                    newFnList.add(new FullNotation(fn.getKnotId(), newCid2, Placement.under, fn.getCidBefore(), newCid1, underStrand, underStrand + 1, Sign.neg));
                }
            }
            if (strandBefore == null) {
                strandBefore = r2SetStrand(fn.getStrandBefore(), smallerStrand, largerStrand);
            }
            if (cidBefore == null) {
                cidBefore = fn.getCidBefore();
            }
            if (cidAfter == null) {
                cidAfter = fn.getCidAfter();
            }
            newFnList.add(new FullNotation(fn.getKnotId(), fn.getCrossingId(), fn.getPlacement(), cidBefore,
                    cidAfter, strandBefore, r2SetStrand(fn.getStrandAfter(), smallerStrand, largerStrand), fn.getSign()));
        }
        return newFnList;
    }

    //Consider splitting into taking strand1,2,3, or switching controller needs
    public boolean triangleR3able(List<FullNotation> fnList, List<Long> bdy) {
        boolean double_over = false;
        boolean double_under = false;
        boolean one_each = false;
        for (Long  strand : bdy) {
            int over = 0;
            int under = 0;
            for (FullNotation fn : fnList) {
                if (fn.getStrandBefore().equals(strand) || fn.getStrandAfter().equals(strand)) {
                    if (fn.getPlacement() == Placement.over) {
                        over += 1;
                    } else {
                        under += 1;
                    }
                    if (over + under == 2) {
                        break;
                    }
                }
            }
            if (over == 2) {
                if (double_over) {
                    return false;
                } else {
                    double_over = true;
                }
            } else if (under == 2) {
                if (double_under) {
                    return false;
                } else {
                    double_under = true;
                }
            } else {
                if (one_each) {
                    return false;
                } else {
                    one_each = true;
                }
            }
        }
        if (double_over && double_under && one_each) {
            return true;
        } else {
            return false;
        }
    }

    public List<List<Long>> r3ableList(List<FullNotation> fnList, List<List<Long>> boundaries) {
        List<List<Long>> r3able = new ArrayList<>();
        for (List<Long> bdy : boundaries) {
            if (bdy.size() ==3) {
                if (triangleR3able(fnList, bdy)) {
                    r3able.add(bdy);
                }
            }
        }
        return r3able;
    }


    public List<FullNotation> r3Slide(List<FullNotation> fnList, List<Long> bdy, Long strand, Long cid) {
        //List<FullNotation> newFnList = new ArrayList<>();
        return fnList;
    }

    //&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& UNTESTED !!!!!!!!!!!!!!!!!!!!!!!!
    public List<FullNotation> saddle(List<FullNotation> fnList, Long strand1, Long strand2) {
        List<FullNotation> newFnList = new ArrayList<>();
        HashMap<Integer, FullNotation> orderedFnToChange = new HashMap<>();
        for (FullNotation fn : fnList) {
            if (fn.getStrandBefore().equals(strand1)) {
                orderedFnToChange.put(0, fn);
            } else if (fn.getStrandAfter().equals(strand1)) {
                orderedFnToChange.put(1, fn);
            } else if (fn.getStrandBefore().equals(strand2)) {
                orderedFnToChange.put(2, fn);
            } else if (fn.getStrandAfter().equals(strand2)) {
                orderedFnToChange.put(3, fn);
            } else {
                newFnList.add(fn);
            }
        }
        FullNotation fn = orderedFnToChange.get(0);
        fn.setCidBefore(orderedFnToChange.get(2).getCidBefore());
        fn.setStrandBefore(strand2);
        newFnList.add(fn);
        fn = orderedFnToChange.get(1);
        fn.setCidAfter(orderedFnToChange.get(3).getCidAfter());
        newFnList.add(fn);
        fn = orderedFnToChange.get(2);
        fn.setCidBefore(orderedFnToChange.get(0).getCidBefore());
        fn.setStrandBefore(strand1);
        newFnList.add(fn);
        fn = orderedFnToChange.get(3);
        fn.setCidAfter(orderedFnToChange.get(1).getCidAfter());
        newFnList.add(fn);
        return newFnList;
    }


    //Using convention :
    //+1: [under into, over outOf, under outOf, over into]
    //neg: [under into, over into, under outOf, over outOf]
    private HashMap<Long, List<LongPair>> getOrderedStrands(List<FullNotation> fnList) {
        HashMap<Long, List<LongPair>> orderedStrands = new HashMap<>();
        for (FullNotation fn : fnList) {
            if (!orderedStrands.containsKey(fn.getCrossingId())) {
                orderedStrands.put(fn.getCrossingId(), new ArrayList<>(Arrays.asList(new LongPair(0L,0L), new LongPair(0L,0L), new LongPair(0L,0L), new LongPair(0L,0L))));
            }
            List<LongPair> currStrands = orderedStrands.get(fn.getCrossingId());
            if (fn.getPlacement() == Placement.over) {
                if (fn.getSign() == Sign.pos) {
                    currStrands.set(1, new LongPair(fn.getStrandAfter(), 0L));
                    currStrands.set(3, new LongPair(fn.getStrandBefore(), 0L));
                } else if (fn.getSign() == Sign.neg) {
                    currStrands.set(1, new LongPair(fn.getStrandBefore(), 0L));
                    currStrands.set(3, new LongPair(fn.getStrandAfter(), 0L));
                }
            } else if (fn.getPlacement() == Placement.under) {
                currStrands.set(0, new LongPair(fn.getStrandBefore(), 0L));
                currStrands.set(2, new LongPair(fn.getStrandAfter(), 0L));
            }
        }
        return orderedStrands;
    }

    private LongPair nextStrandnextCid(Long strand, List<FullNotation> fnList) {
        for (FullNotation fn : fnList) {
            if (fn.getStrandBefore().equals(strand)) {
                return new LongPair(fn.getStrandAfter(), fn.getCrossingId());
            }
        }
        return null;
    }

    public HashMap<Long, LongPair> getStrandMapping(List<FullNotation> fnList) {
        HashMap<Long, LongPair> strandMapping = new HashMap<>();
        for (FullNotation fn : fnList) {
            strandMapping.putIfAbsent(fn.getStrandBefore(), new LongPair(null, null));
            strandMapping.putIfAbsent(fn.getStrandAfter(), new LongPair(null, null));
            strandMapping.put(fn.getStrandBefore(), new LongPair(fn.getCidBefore(), strandMapping.get(fn.getStrandBefore()).y()));
            strandMapping.put(fn.getStrandAfter(), new LongPair(strandMapping.get(fn.getStrandAfter()).x(), fn.getCidAfter()));
        }
        return strandMapping;
    }

    public List<Long> getCidList(List<FullNotation> fnList) {
        List<Long> cidList = new ArrayList<>();
        LongPair nxtSnxtC = nextStrandnextCid(0L, fnList);
        cidList.add(nxtSnxtC.y());
        Long strand = nxtSnxtC.x();
        while (strand != 0L) {
            nxtSnxtC = nextStrandnextCid(strand, fnList);
            cidList.add(nxtSnxtC.y());
            strand = nxtSnxtC.x();
        }
        return cidList;
    }

    private HashMap<Long, List<LongPair>> remUsedCids(HashMap<Long, List<LongPair>> orderedStrands) {
        HashMap<Long, List<LongPair>> newOrderedStrands = new HashMap<>();
        for (Map.Entry<Long, List<LongPair>> entry : orderedStrands.entrySet()) {
            List<LongPair> strands = entry.getValue();
            for (LongPair strand : strands) {
                if (!strand.y().equals(2L)) {
                    newOrderedStrands.put(entry.getKey(), strands);
                    break;
                }
            }
        }
        return newOrderedStrands;
    }

    private int plus1Mod4(int n) {
        return (n + 1) % 4;
    }

    private LongPair findEqualStrand(HashMap<Long, List<LongPair>> orderedStrands, Long ogCid, Long ogStrand) {
        for (Map.Entry<Long, List<LongPair>> orderedStrand : orderedStrands.entrySet()) {
            if (!orderedStrand.getKey().equals(ogCid)) {
                for (int i = 0; i < 4; i++) {
                    if (orderedStrand.getValue().get(i).x().equals(ogStrand)) {
                        return new LongPair(orderedStrand.getKey(), (long) i);
                    }
                }
            }
        }
        return null;
    }


    private void updateStrandUses(HashMap<Long, List<LongPair>> orderedStrands, List<LongPair> newBdyRaw) {
        List<Long> newBdy = new ArrayList<>();
        for (LongPair raw : newBdyRaw) {
            newBdy.add(raw.x());
        }
        for (List<LongPair> strands : orderedStrands.values()) {
            for (int i = 0; i < strands.size(); i++) {
                LongPair strand = strands.get(i);
                if (newBdy.contains(strand.x())) {
                    strands.set(i, new LongPair(strand.x(), strand.y() + 1));
                }
            }
        }
    }

    private List<LongPair> findBoundary(HashMap<Long, List<LongPair>> orderedStrands, Long nextCid, int startingStrandIndex, List<Long> cidList) {
        List<LongPair> bdy = new ArrayList<>();
        //nextStart is just one entry of { cid : strand }
        Long endStrand = orderedStrands.get(nextCid).get(startingStrandIndex).x();
        Long nextStrand = orderedStrands.get(nextCid).get(plus1Mod4(startingStrandIndex)).x();
        bdy.add(new LongPair(nextStrand, nextCid));
        //Long lastCid = nextCid;
        while (!nextStrand.equals(endStrand)) {
            // (cid, index)
            LongPair equalStrand = findEqualStrand(orderedStrands, nextCid, nextStrand);
            nextCid = equalStrand.x();
            //if (orientationWithKnot(lastCid, nextCid, cidList));
            int nextIndex = plus1Mod4(equalStrand.y().intValue());
            nextStrand = orderedStrands.get(nextCid).get(nextIndex).x();
            bdy.add(new LongPair(nextStrand, nextCid));
            //lastCid = nextCid;
        }
        return bdy;
    }

    private List<LongPair> orientBoundary(List<LongPair> bdyRaw, HashMap<Long, LongPair> strandMapping) {
        List<LongPair> orientedBoundary = new ArrayList<>();
        for (int i = 0; i < bdyRaw.size(); i++) {
            Long strand = bdyRaw.get(i).x();
            Long cid = bdyRaw.get(i).y();
            Long nextCid = bdyRaw.get((i + 1) % bdyRaw.size()).y();
            LongPair direction = strandMapping.get(strand);
            if (direction.x().equals(cid) && direction.y().equals(nextCid)) {
                orientedBoundary.add(new LongPair(strand, 1L));
            } else if (direction.x().equals(nextCid) && direction.y().equals(cid)) {
                orientedBoundary.add(new LongPair(strand, 0L));
            }
        }
        return orientedBoundary;
    }


    public List<List<LongPair>> getOrientedBoundaries(List<FullNotation> fnList) {
        HashMap<Long, List<LongPair>> orderedStrands = getOrderedStrands(fnList);
        //orderedStrands is { cid : ((a,0),(b,0),(c,0),(d,0)) } where a,b,c,d are strands

        List<Long> cidList = getCidList(fnList);
        List<List<LongPair>> boundaries = new ArrayList<>(); //Should soon be List<List<Pair<Long, Boolean>>> for orientation
        int numFaces = orderedStrands.size() + 2;
        while (boundaries.size() < numFaces) {
            Map.Entry<Long, List<LongPair>> entry = orderedStrands.entrySet().iterator().next();
            int strandIndex = -1;
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (entry.getValue().get(i).y().equals(0L)) {
                    strandIndex = i;
                    break;
                }
            }
            if (strandIndex == -1) {
                for (int i = 0; i < entry.getValue().size(); i++) {
                    if (entry.getValue().get(i).y().equals(1L) && entry.getValue().get(plus1Mod4(i)).y().equals(1L)) {
                        strandIndex = i;
                    }
                }
            }
            List<LongPair> newBdyRaw = findBoundary(orderedStrands, entry.getKey(), strandIndex, cidList);
            updateStrandUses(orderedStrands, newBdyRaw);
            HashMap<Long, LongPair> strandMapping = getStrandMapping(fnList);
            List<LongPair> newBdyOriented = orientBoundary(newBdyRaw, strandMapping);
            boundaries.add(newBdyOriented);
            orderedStrands = remUsedCids(orderedStrands);
        }
        return boundaries;
    }

    public HashMap<Long, Boolean> orientAllStrands(List<FullNotation> fnList) {
        HashMap<Long, LongPair> strandMapping = getStrandMapping(fnList);
        HashMap<Long, Boolean> orientedStrands = new HashMap<>();

        for (Map.Entry<Long, LongPair> entry : strandMapping.entrySet()) {
            orientedStrands.put(entry.getKey(), true);
        }
        return orientedStrands;
    }




}
