package com.knots.backend.services;

import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.Placement;


import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FnStrand;
import com.knots.backend.models.entities.FullNotation;

import java.util.*;
import java.util.stream.Collectors;

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

    private Long twistSetStrand(Long twistStrand, Long currStrand) {
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
                nextLine.setStrandAfter(twistSetStrand(strand, fn.getStrandAfter()));
                nextLine.setCidBefore(twistCid);
                nextLine.setCidAfter(fn.getCidAfter());
                cidBefore = fn.getCrossingId();
            } else if (fn.getStrandAfter().equals(strand)) {
                nextLine.setStrandAfter(strand);
                nextLine.setCidAfter(twistCid);
                nextLine.setCidBefore(fn.getCidBefore());
                cidAfter = fn.getCrossingId();
            } else {
                nextLine.setCidBefore(fn.getCidBefore());
                nextLine.setCidAfter(fn.getCidAfter());
                nextLine.setStrandAfter(twistSetStrand(strand, fn.getStrandAfter()));
            }
            nextLine.setStrandBefore(twistSetStrand(strand, fn.getStrandBefore()));
            nextLine.setSign(fn.getSign());
            newFnList.add(nextLine);
        }
        // cidAfter holds the crossing where `strand` starts (its row's cidAfter now points at the twist)
        // and cidBefore the crossing where it ends, so the twist enters from cidAfter and leaves to cidBefore.
        if (sign == Sign.pos) {
            newFnList.add(new FullNotation(kId, twistCid, Placement.over, cidAfter, twistCid, strand, strand+1, sign));
            newFnList.add(new FullNotation(kId, twistCid, Placement.under, twistCid, cidBefore, strand+1, strand+2, sign));
        } else if (sign == Sign.neg) {
            newFnList.add(new FullNotation(kId, twistCid, Placement.under, cidAfter, twistCid, strand, strand+1, sign));
            newFnList.add(new FullNotation(kId, twistCid, Placement.over, twistCid, cidBefore, strand+1, strand+2, sign));
        }
        return newFnList;
    }


    //*************************** !!!!!!!!!!!!!!!!!!!!!!!!!!
    //Set logic should be in one function with one more integer, the +/- amount
    private Long addPokeSetStrand(Long strand, Long strandA, Long strandB) {
        Long strand1;
        Long strand2;
        if (strandA < strandB) {
            strand1 = strandA;
            strand2 = strandB;
        } else {
            strand1 = strandB;
            strand2 = strandA;
        }
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

    private FullNotation copy(FullNotation fn) {
        return new FullNotation(fn.getKnotId(), fn.getCrossingId(), fn.getPlacement(),
                fn.getCidBefore(), fn.getCidAfter(), fn.getStrandBefore(), fn.getStrandAfter(), fn.getSign());
    }

    private FullNotation strandSource(List<FullNotation> rows, Long strand) {
        return rows.stream().filter(fn -> fn.getStrandAfter().equals(strand)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strand: " + strand));
    }

    private FullNotation strandTarget(List<FullNotation> rows, Long strand) {
        return rows.stream().filter(fn -> fn.getStrandBefore().equals(strand)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strand: " + strand));
    }

    public List<FullNotation> addPoke(List<FullNotation> fnList, LongPair underStrandOriented, LongPair overStrandOriented) {
        long under = underStrandOriented.x();
        long over = overStrandOriented.x();
        if (under == over) {
            throw new IllegalArgumentException("Poke requires two distinct strands");
        }
        FullNotation underSource = strandSource(fnList, under);
        FullNotation underTarget = strandTarget(fnList, under);
        FullNotation overSource = strandSource(fnList, over);
        FullNotation overTarget = strandTarget(fnList, over);
        long first = fnList.stream().mapToLong(FullNotation::getCrossingId).max().orElse(0L) + 1;
        long second = first + 1;
        boolean sameDirection = underStrandOriented.y().equals(overStrandOriented.y());
        // Boundary directions choose the face side and the crossing order on each strand.
        Sign firstSign = underStrandOriented.y() == 1L ? Sign.neg : Sign.pos;
        long underFirst = sameDirection ? second : first;
        long underSecond = sameDirection ? first : second;
        long smaller = Math.min(under, over);
        long larger = Math.max(under, over);
        // A split starts after all insertions on lower-numbered strands.
        long u = addPokeSetStrand(under, smaller, larger);
        long o = addPokeSetStrand(over, smaller, larger);
        List<FullNotation> result = new ArrayList<>();
        for (FullNotation original : fnList) {
            FullNotation row = copy(original);
            long before = original.getStrandBefore();
            long after = original.getStrandAfter();
            row.setStrandBefore(addPokeSetStrand(before, smaller, larger));
            row.setStrandAfter(addPokeSetStrand(after, smaller, larger));
            if (before == under) { row.setStrandBefore(u + 2); row.setCidBefore(underSecond); }
            if (before == over) { row.setStrandBefore(o + 2); row.setCidBefore(second); }
            if (after == under) { row.setCidAfter(underFirst); }
            if (after == over) { row.setCidAfter(first); }
            result.add(row);
        }
        long knotId = overSource.getKnotId();
        result.add(new FullNotation(knotId, first, Placement.over, overSource.getCrossingId(), second, o, o + 1, firstSign));
        result.add(new FullNotation(knotId, second, Placement.over, first, overTarget.getCrossingId(), o + 1, o + 2, firstSign.opposite()));
        result.add(new FullNotation(knotId, underFirst, Placement.under, underSource.getCrossingId(), underSecond, u, u + 1, sameDirection ? firstSign.opposite() : firstSign));
        result.add(new FullNotation(knotId, underSecond, Placement.under, underFirst, underTarget.getCrossingId(), u + 1, u + 2, sameDirection ? firstSign : firstSign.opposite()));
        return result;
    }

    //Consider splitting into taking strand1,2,3, or switching controller needs
    public boolean triangleBdySlideable(List<FullNotation> fnList, List<Long> bdy) {
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

    public List<List<Long>> slideableList(List<FullNotation> fnList, List<List<Long>> boundaries) {
        List<List<Long>> slideable = new ArrayList<>();
        for (List<Long> bdy : boundaries) {
            // isSlideableTriangle includes triangleBdySlideable and also rejects degenerate faces
            // (e.g. next to a twist loop) that slideTriangle can't move.
            if (bdy.size() == 3 && isSlideableTriangle(fnList, bdy)) {
                slideable.add(bdy);
            }
        }
        return slideable;
    }

    // Slide (Reidemeister III) of `strand` across crossing `cid`, the crossing of the triangle that
    // isn't on `strand`. Finds the other two strands of the triangle, then calls slideTriangle.
    public List<FullNotation> Slide(List<FullNotation> fnList, Long strand, Long cid) {
        Map<Long, FullNotation> byStrandBefore = new HashMap<>();
        Map<Long, FullNotation> byStrandAfter = new HashMap<>();
        for (FullNotation fn : fnList) {
            byStrandBefore.put(fn.getStrandBefore(), fn);
            byStrandAfter.put(fn.getStrandAfter(), fn);
        }
        if (!byStrandBefore.containsKey(strand) || !byStrandAfter.containsKey(strand)) {
            throw new IllegalArgumentException("Unknown strand: " + strand);
        }
        Long startCid = byStrandAfter.get(strand).getCrossingId();
        Long endCid = byStrandBefore.get(strand).getCrossingId();
        List<Long> toStart = new ArrayList<>();
        List<Long> toEnd = new ArrayList<>();
        for (Long other : byStrandBefore.keySet()) {
            if (other.equals(strand) || !byStrandAfter.containsKey(other)) {
                continue;
            }
            Set<Long> ends = new HashSet<>(List.of(byStrandAfter.get(other).getCrossingId(), byStrandBefore.get(other).getCrossingId()));
            if (ends.size() == 2 && ends.contains(cid)) {
                if (ends.contains(startCid)) toStart.add(other);
                if (ends.contains(endCid)) toEnd.add(other);
            }
        }
        for (Long t : toStart) {
            for (Long u : toEnd) {
                List<Long> triangle = List.of(strand, t, u);
                if (isSlideableTriangle(fnList, triangle)) {
                    return slideTriangle(fnList, triangle);
                }
            }
        }
        throw new IllegalArgumentException("Strand " + strand + " and crossing " + cid + " don't form a slideable triangle");
    }

    // True when the three strands bound a triangle that can be slid: three different strands
    // joining three different crossings pairwise, with one strand over at both of its crossings,
    // one under at both, and one of each (triangleBdySlideable).
    public boolean isSlideableTriangle(List<FullNotation> fnList, List<Long> triangle) {
        if (triangle == null || triangle.size() != 3 || new HashSet<>(triangle).size() != 3) {
            return false;
        }
        Set<FullNotation> passages = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Set<Long>> sides = new HashSet<>();
        Set<Long> crossings = new HashSet<>();
        for (Long strand : triangle) {
            List<FullNotation> into = fnList.stream().filter(fn -> fn.getStrandAfter().equals(strand)).toList();
            List<FullNotation> outOf = fnList.stream().filter(fn -> fn.getStrandBefore().equals(strand)).toList();
            if (into.size() != 1 || outOf.size() != 1) {
                return false;
            }
            Long start = into.get(0).getCrossingId();
            Long end = outOf.get(0).getCrossingId();
            if (start.equals(end)) {
                return false;
            }
            passages.add(into.get(0));
            passages.add(outOf.get(0));
            sides.add(Set.of(start, end));
            crossings.add(start);
            crossings.add(end);
        }
        // 6 different passages means no strand of the triangle runs straight into another one.
        return passages.size() == 6 && sides.size() == 3 && crossings.size() == 3
                && triangleBdySlideable(fnList, triangle);
    }

    // Slide (Reidemeister III) on a triangle given by its three strands. Along each of the three
    // strands, the crossings at its two ends swap order; every crossing keeps its placement and
    // sign, and every strand keeps its number. Returns new rows in the same order; fnList is not modified.
    public List<FullNotation> slideTriangle(List<FullNotation> fnList, List<Long> triangle) {
        if (!isSlideableTriangle(fnList, triangle)) {
            throw new IllegalArgumentException("Not a slideable triangle: " + triangle);
        }
        Map<Long, FullNotation> byStrandBefore = new HashMap<>();
        for (FullNotation fn : fnList) {
            byStrandBefore.put(fn.getStrandBefore(), fn);
        }
        Map<FullNotation, FullNotation> replacement = new IdentityHashMap<>();
        Set<FullNotation> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FullNotation start : fnList) {
            if (visited.contains(start)) {
                continue;
            }
            // Walk one component: passages[i] is followed by strands[i], then passages[i + 1].
            List<FullNotation> passages = new ArrayList<>();
            FullNotation current = start;
            while (current != null && visited.add(current)) {
                passages.add(current);
                current = byStrandBefore.get(current.getStrandAfter());
            }
            int k = passages.size();
            List<Long> strands = passages.stream().map(FullNotation::getStrandAfter).toList();
            List<FullNotation> order = new ArrayList<>(passages);
            for (int i = 0; i < k; i++) {
                if (triangle.contains(strands.get(i))) {
                    Collections.swap(order, i, (i + 1) % k);
                }
            }
            for (int i = 0; i < k; i++) {
                FullNotation fn = order.get(i);
                replacement.put(fn, new FullNotation(fn.getKnotId(), fn.getCrossingId(), fn.getPlacement(),
                        order.get((i - 1 + k) % k).getCrossingId(), order.get((i + 1) % k).getCrossingId(),
                        strands.get((i - 1 + k) % k), strands.get(i), fn.getSign()));
            }
        }
        return fnList.stream().map(replacement::get).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<FullNotation> saddle(List<FullNotation> fnList, Long strand1, Long strand2) {
        if (Objects.equals(strand1, strand2)) {
            throw new IllegalArgumentException("Saddle requires two distinct strands");
        }
        FullNotation source1 = strandSource(fnList, strand1);
        FullNotation source2 = strandSource(fnList, strand2);
        FullNotation target1 = strandTarget(fnList, strand1);
        FullNotation target2 = strandTarget(fnList, strand2);
        List<FullNotation> result = new ArrayList<>();
        for (FullNotation original : fnList) {
            FullNotation row = copy(original);
            if (original.getStrandBefore().equals(strand1)) {
                row.setStrandBefore(strand2);
                row.setCidBefore(source2.getCrossingId());
            }
            if (original.getStrandBefore().equals(strand2)) {
                row.setStrandBefore(strand1);
                row.setCidBefore(source1.getCrossingId());
            }
            if (original.getStrandAfter().equals(strand1)) {
                row.setCidAfter(target2.getCrossingId());
            }
            if (original.getStrandAfter().equals(strand2)) {
                row.setCidAfter(target1.getCrossingId());
            }
            result.add(row);
        }
        return result;
    }

    // Crossing change at cid: the strand that went over now goes under and vice versa. With the
    // orientations unchanged this flips the crossing's sign (like mirror, for one crossing).
    // Returns new rows; fnList is not modified.
    public List<FullNotation> switchPlacement(List<FullNotation> fnList, Long cid) {
        List<FullNotation> result = new ArrayList<>();
        for (FullNotation fn : fnList) {
            FullNotation row = copy(fn);
            if (fn.getCrossingId().equals(cid)) {
                row.setPlacement(fn.getPlacement() == Placement.over ? Placement.under : Placement.over);
                row.setSign(fn.getSign().opposite());
            }
            result.add(row);
        }
        return result;
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
        Map<Long, List<LongPair>> crossings = getOrderedStrands(fnList);
        Map<Long, List<LongPair>> endpoints = new HashMap<>();
        for (Long cid : new TreeSet<>(crossings.keySet())) {
            for (int slot = 0; slot < 4; slot++) {
                long strand = crossings.get(cid).get(slot).x();
                endpoints.computeIfAbsent(strand, ignored -> new ArrayList<>()).add(new LongPair(cid, (long) slot));
            }
        }
        Set<LongPair> visited = new HashSet<>();
        List<List<LongPair>> boundaries = new ArrayList<>();
        for (Long cid : new TreeSet<>(crossings.keySet())) {
            for (int slot = 0; slot < 4; slot++) {
                LongPair start = new LongPair(cid, (long) slot);
                if (visited.contains(start)) { continue; }
                LongPair current = start;
                List<LongPair> boundary = new ArrayList<>();
                do {
                    if (!visited.add(current)) {
                        throw new IllegalArgumentException("Invalid diagram boundary");
                    }
                    long strand = crossings.get(current.x()).get(current.y().intValue()).x();
                    List<LongPair> ends = endpoints.get(strand);
                    if (ends.size() != 2) {
                        throw new IllegalArgumentException("Strand must have exactly two endpoints: " + strand);
                    }
                    LongPair other = ends.get(0).equals(current) ? ends.get(1) : ends.get(0);
                    FullNotation source = strandSource(fnList, strand);
                    int outgoingSlot = source.getPlacement() == Placement.under ? 2
                            : source.getSign() == Sign.pos ? 1 : 3;
                    boolean forward = source.getCrossingId().equals(current.x()) && current.y() == outgoingSlot;
                    boundary.add(new LongPair(strand, forward ? 1L : 0L));
                    current = new LongPair(other.x(), (other.y() + 1) % 4);
                } while (!current.equals(start));
                boundaries.add(boundary);
            }
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

    //****************************************************************************
    // Removing twists (R1) and pokes (R2)

    // A twist: a strand that leaves a crossing and comes straight back to it. Its two ends are the
    // crossing's two different passages, so the loop always bounds a one-strand region.
    public List<Long> getAllTwisted(List<FullNotation> fnList) {
        Map<Long, FullNotation> byStrandBefore = new HashMap<>();
        for (FullNotation fn : fnList) {
            byStrandBefore.put(fn.getStrandBefore(), fn);
        }
        Set<Long> twists = new TreeSet<>();
        for (FullNotation fn : fnList) {
            FullNotation end = byStrandBefore.get(fn.getStrandAfter());
            if (end != null && end != fn && end.getCrossingId().equals(fn.getCrossingId())) {
                twists.add(fn.getCrossingId());
            }
        }
        return new ArrayList<>(twists);
    }

    // A poke: a region bounded by exactly two strands joining the same two crossings, one strand
    // over at both of its ends and the other under at both. Returns each pair of crossings once,
    // smaller id first.
    public List<LongPair> getAllPoked(List<FullNotation> fnList) {
        if (fnList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, FullNotation> byStrandBefore = new HashMap<>();
        Map<Long, FullNotation> byStrandAfter = new HashMap<>();
        for (FullNotation fn : fnList) {
            byStrandBefore.put(fn.getStrandBefore(), fn);
            byStrandAfter.put(fn.getStrandAfter(), fn);
        }
        Set<LongPair> pokes = new LinkedHashSet<>();
        for (List<LongPair> boundary : getOrientedBoundaries(fnList)) {
            List<Long> strands = boundary.stream().map(LongPair::x).distinct().toList();
            if (strands.size() != 2) {
                continue;
            }
            FullNotation start1 = byStrandAfter.get(strands.get(0)), end1 = byStrandBefore.get(strands.get(0));
            FullNotation start2 = byStrandAfter.get(strands.get(1)), end2 = byStrandBefore.get(strands.get(1));
            Set<Long> ends1 = new HashSet<>(List.of(start1.getCrossingId(), end1.getCrossingId()));
            Set<Long> ends2 = new HashSet<>(List.of(start2.getCrossingId(), end2.getCrossingId()));
            boolean sameCrossings = ends1.size() == 2 && ends1.equals(ends2);
            boolean oneOverOneUnder = start1.getPlacement() == end1.getPlacement()
                    && start2.getPlacement() == end2.getPlacement()
                    && start1.getPlacement() != start2.getPlacement();
            if (sameCrossings && oneOverOneUnder) {
                long a = start1.getCrossingId(), b = end1.getCrossingId();
                pokes.add(new LongPair(Math.min(a, b), Math.max(a, b)));
            }
        }
        return new ArrayList<>(pokes);
    }

    // Undo a twist (Reidemeister I) at crossing cid.
    public List<FullNotation> remTwist(List<FullNotation> fnList, Long cid) {
        if (!getAllTwisted(fnList).contains(cid)) {
            throw new IllegalArgumentException("Crossing " + cid + " is not a twist");
        }
        return removeCrossings(fnList, Set.of(cid));
    }

    // Undo a poke (Reidemeister II) between crossings cid1 and cid2.
    public List<FullNotation> remPoke(List<FullNotation> fnList, Long cid1, Long cid2) {
        LongPair pair = new LongPair(Math.min(cid1, cid2), Math.max(cid1, cid2));
        if (!getAllPoked(fnList).contains(pair)) {
            throw new IllegalArgumentException("Crossings " + cid1 + " and " + cid2 + " are not a poke");
        }
        return removeCrossings(fnList, Set.of(cid1, cid2));
    }

    // Remove twists and pokes one at a time (always a twist first if there is one) until none are left.
    public List<FullNotation> remPokesAndTwists(List<FullNotation> fnList) {
        List<FullNotation> current = fnList.stream().map(this::copy).collect(Collectors.toCollection(ArrayList::new));
        boolean removed = true;
        while (removed && !current.isEmpty()) {
            removed = false;
            List<Long> twists = getAllTwisted(current);
            if (!twists.isEmpty()) {
                current = remTwist(current, twists.get(0));
                removed = true;
                continue;
            }
            for (LongPair poke : getAllPoked(current)) {
                try {
                    current = remPoke(current, poke.x(), poke.y());
                    removed = true;
                    break;
                } catch (IllegalArgumentException e) {
                    // This poke would leave a link component with no crossings; try the next one.
                }
            }
        }
        return current;
    }

    // Remove every passage through the given crossings and join up what is left: along each component,
    // the strand that went into a removed run of passages carries on to the next passage kept. Strands
    // and crossings are then renumbered so their ids stay contiguous (starting where they started).
    // Returns an empty list when nothing is left (the unknot). Refuses to drop a whole link component
    // while others remain, since FullNotation can't hold a component without crossings.
    private List<FullNotation> removeCrossings(List<FullNotation> fnList, Set<Long> cids) {
        Map<Long, FullNotation> byStrandBefore = new HashMap<>();
        for (FullNotation fn : fnList) {
            byStrandBefore.put(fn.getStrandBefore(), fn);
        }
        Set<FullNotation> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        List<FullNotation> rebuilt = new ArrayList<>();
        int droppedComponents = 0;
        int components = 0;
        for (FullNotation start : fnList) {
            if (visited.contains(start)) {
                continue;
            }
            components++;
            List<FullNotation> component = new ArrayList<>();
            FullNotation current = start;
            while (current != null && visited.add(current)) {
                component.add(current);
                current = byStrandBefore.get(current.getStrandAfter());
            }
            List<FullNotation> kept = component.stream().filter(fn -> !cids.contains(fn.getCrossingId())).toList();
            if (kept.isEmpty()) {
                droppedComponents++;
                continue;
            }
            int k = kept.size();
            for (int i = 0; i < k; i++) {
                FullNotation fn = kept.get(i);
                FullNotation previous = kept.get((i - 1 + k) % k);
                FullNotation next = kept.get((i + 1) % k);
                rebuilt.add(new FullNotation(fn.getKnotId(), fn.getCrossingId(), fn.getPlacement(),
                        previous.getCrossingId(), next.getCrossingId(), previous.getStrandAfter(), fn.getStrandAfter(), fn.getSign()));
            }
        }
        if (droppedComponents > 0 && droppedComponents < components) {
            throw new IllegalArgumentException("Removing crossings " + cids + " would leave a link component with no crossings");
        }
        return renumber(rebuilt, fnList);
    }

    // Give strands and crossings contiguous ids in their existing order, starting from the smallest ids
    // the original diagram used.
    private List<FullNotation> renumber(List<FullNotation> rows, List<FullNotation> original) {
        if (rows.isEmpty()) {
            return rows;
        }
        long firstStrand = original.stream().mapToLong(FullNotation::getStrandAfter).min().orElse(0L);
        long firstCrossing = original.stream().mapToLong(FullNotation::getCrossingId).min().orElse(1L);
        Map<Long, Long> strandIds = new HashMap<>();
        rows.stream().map(FullNotation::getStrandAfter).sorted().forEach(id -> strandIds.put(id, firstStrand + strandIds.size()));
        Map<Long, Long> crossingIds = new HashMap<>();
        rows.stream().map(FullNotation::getCrossingId).distinct().sorted().forEach(id -> crossingIds.put(id, firstCrossing + crossingIds.size()));
        return rows.stream().map(fn -> new FullNotation(fn.getKnotId(), crossingIds.get(fn.getCrossingId()), fn.getPlacement(),
                        crossingIds.get(fn.getCidBefore()), crossingIds.get(fn.getCidAfter()),
                        strandIds.get(fn.getStrandBefore()), strandIds.get(fn.getStrandAfter()), fn.getSign()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
