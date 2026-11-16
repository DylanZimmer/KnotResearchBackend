package com.knots.backend.services;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.MoveRequestArgs;
import com.knots.backend.models.dtos.Placement;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.experiments.*;
import com.knots.backend.models.entities.FullNotation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

//StrandType
    //CrossingType, CrossingType, Boolean
//CrossingType
    //Long, Position

@RequiredArgsConstructor
@Service
public class LocationSpecifyingService {

    private final FullNotationMovesService fullNotationMovesService;

    private List<Long> intersectPotentialLongLists(List<List<Long>> potentialStrandLists) {
        if (potentialStrandLists.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> intersection = new ArrayList<>(potentialStrandLists.get(0));
        for (int i = 1; i < potentialStrandLists.size(); i++) {
            intersection.retainAll(potentialStrandLists.get(i));
        }
        return intersection;
    }


    public List<Long> identifyAllCids(List<FullNotation> fnList, Sign sign, Placement placement) {
        List<List<Long>> potentialCidLists = new ArrayList<>();

        if (sign != null) {
            List<Long> potentialCids = new ArrayList<>();
            for (FullNotation fn : fnList) {
                if (fn.getSign().equals(sign)) {
                    potentialCids.add(fn.getCrossingId());
                }
            }
            potentialCidLists.add(potentialCids);
        }

        if (placement != null) {
            List<Long> potentialCids = new ArrayList<>();
            for (FullNotation fn : fnList) {
                if (fn.getPlacement().equals(placement)) {  //MAKE SURE THIS IS OK WITH PLACEMENT ENUM AND DB TYPE
                    potentialCids.add(fn.getCrossingId());
                }
            }
            potentialCidLists.add(potentialCids);
        }

        return intersectPotentialLongLists(potentialCidLists);
    }

    public Long identifyOneCid(List<FullNotation> fnList, Sign sign, Placement placement) {
        List<Long> allCids = identifyAllCids(fnList, sign, placement);
        return allCids.isEmpty() ? null : allCids.get(0);
    }


    // StrandPair
    //public List<Long> identifyAllPossiblePokes(List<FullNotation> fnList, StrandTypeDto strandType, HashMap<Long, Boolean> orientedStrands, List<List<LongPair>> orientedBoundaries) {
    /*
    public List<LongPair> identifyAllPossiblePokes(List<FullNotation> fnList, BoundaryTypeDto boundaryType, HashMap<Long, Boolean> orientedStrands, List<List<LongPair>> orientedBoundaries) {
        List<LongPair> allPossiblePokes = new ArrayList<>();
        //First find the potential boundaries, if there's non-strand or non-crossing
        if (boundaryType.numCrossings() != null) {
            Iterator<List<LongPair>> iterator = orientedBoundaries.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().size() != boundaryType.numCrossings()) {
                    iterator.remove();
                }
            }
        }
        if (boundaryType.strands() != null) {
            for (StrandTypeDto strand : boundaryType.strands()) {
                if
            }
        }
        HashMap<Long, Boolean> potentialStrands1 = checkStrand(fnList, strandPair.strand1(), orientedStrands);
        HashMap<Long, Boolean> potentialStrands2 = checkStrand(fnList, strandPair.strand2(), orientedStrands);
        for (List<LongPair> ob : orientedBoundaries) {
            Set<Long> obStrands = ob.stream().map(LongPair::x).collect(Collectors.toSet());
            List<Long> keptStrands1 = potentialStrands1.keySet().stream().filter(obStrands::contains).toList();
            List<Long> keptStrands2 = potentialStrands2.keySet().stream().filter(obStrands::contains).toList();
            allPossiblePokes.addAll(keptStrands1.stream().flatMap(s1 -> keptStrands2.stream()
                    .filter(s2 -> !s1.equals(s2)).map(s2 -> new LongPair(s1, s2))).toList());
        }
        return allPossiblePokes;
    }
    */

    public List<LongPair> identifyAllPossiblePokes(List<FullNotation> fnList, BoundaryTypeDto boundaryType, HashMap<Long, Boolean> orientedStrands, List<List<LongPair>> orientedBoundariesArg) {

        List<LongPair> allPossiblePokes = new ArrayList<>();
        List<List<LongPair>> orientedBoundaries = new ArrayList<>(orientedBoundariesArg);
        if (boundaryType == null) {
            return identifyAllPossiblePokes(fnList, new BoundaryTypeDto(null, null, null), orientedStrands, orientedBoundariesArg);
        }

        // Filter by number of crossings.
        if (boundaryType.numCrossings() != null) {
            Iterator<List<LongPair>> iterator = orientedBoundaries.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().size() != boundaryType.numCrossings()) {
                    iterator.remove();
                }
            }
        }
        // Filter by strand types.
        if (boundaryType.strands() != null && !boundaryType.strands().isEmpty()) {
            Iterator<List<LongPair>> iterator = orientedBoundaries.iterator();
            while (iterator.hasNext()) {
                List<LongPair> boundary = iterator.next();
                Set<Long> boundaryStrands = boundary.stream().map(LongPair::x).collect(Collectors.toSet());
                boolean matchesAllStrandTypes = boundaryType.strands().stream()
                        .allMatch(strandType ->
                                checkStrand(fnList, strandType, orientedStrands)
                                        .keySet().stream().anyMatch(boundaryStrands::contains));
                if (!matchesAllStrandTypes) {
                    iterator.remove();
                }
            }
        }

        // Filter by crossing types.
        if (boundaryType.crossings() != null && !boundaryType.crossings().isEmpty()) {
            Iterator<List<LongPair>> iterator = orientedBoundaries.iterator();
            while (iterator.hasNext()) {
                List<LongPair> boundary = iterator.next();
                Set<Long> boundaryStrands = boundary.stream().map(LongPair::x).collect(Collectors.toSet());
                Set<Long> boundaryCids = fnList.stream()
                        .filter(fn -> boundaryStrands.contains(fn.getStrandBefore())
                                || boundaryStrands.contains(fn.getStrandAfter()))
                        .map(FullNotation::getCrossingId)
                        .collect(Collectors.toSet());

                boolean matchesAllCrossingTypes = boundaryType.crossings().stream()
                        .allMatch(crossingType ->
                                fnList.stream()
                                        .filter(fn -> boundaryCids.contains(fn.getCrossingId()))
                                        .anyMatch(fn ->
                                                (crossingType.sign() == null || fn.getSign() == crossingType.sign())
                                                && (crossingType.placement() == null || fn.getPlacement() == crossingType.placement())));

                if (!matchesAllCrossingTypes) {
                    iterator.remove();
                }
            }
        }
        // Generate distinct strand pairs from the remaining boundaries.
        for (List<LongPair> boundary : orientedBoundaries) {
            Set<Long> boundaryStrands = boundary.stream().map(LongPair::x).collect(Collectors.toSet());
            List<Long> strands = new ArrayList<>(new TreeSet<>(boundaryStrands));
            for (int i = 0; i < strands.size(); i++) {
                for (int j = i + 1; j < strands.size(); j++) {
                    allPossiblePokes.add(new LongPair(strands.get(i), strands.get(j)));
                }
            }
        }
        return allPossiblePokes;
    }

    public List<LongPair> identifyAllPossibleSaddles(List<FullNotation> fnList, BoundaryTypeDto boundaryType, HashMap<Long, Boolean> orientedStrands, List<List<LongPair>> orientedBoundariesArg) {
        List<LongPair> allPossibleSaddles = new ArrayList<>();
        List<List<LongPair>> orientedBoundaries = new ArrayList<>(orientedBoundariesArg);
        if (boundaryType == null) {
            return identifyAllPossibleSaddles(fnList, new BoundaryTypeDto(null, null, null), orientedStrands, orientedBoundariesArg);
        }
        if (boundaryType.numCrossings() != null) {
            orientedBoundaries.removeIf(boundary -> boundary.size() != boundaryType.numCrossings());
        }
        // Filter by strand types.
        if (boundaryType.strands() != null && !boundaryType.strands().isEmpty()) {
            Iterator<List<LongPair>> iterator = orientedBoundaries.iterator();
            while (iterator.hasNext()) {
                List<LongPair> boundary = iterator.next();
                Set<Long> boundaryStrands = boundary.stream()
                        .map(LongPair::x)
                        .collect(Collectors.toSet());

                boolean matchesAllStrandTypes = boundaryType.strands().stream()
                        .allMatch(strandType ->
                                checkStrand(fnList, strandType, orientedStrands)
                                        .keySet()
                                        .stream()
                                        .anyMatch(boundaryStrands::contains));

                if (!matchesAllStrandTypes) {
                    iterator.remove();
                }
            }
        }
        // Filter by crossing types.
        if (boundaryType.crossings() != null && !boundaryType.crossings().isEmpty()) {
            Iterator<List<LongPair>> iterator = orientedBoundaries.iterator();
            while (iterator.hasNext()) {
                List<LongPair> boundary = iterator.next();
                Set<Long> boundaryStrands = boundary.stream()
                        .map(LongPair::x)
                        .collect(Collectors.toSet());
                Set<Long> boundaryCids = fnList.stream()
                        .filter(fn -> boundaryStrands.contains(fn.getStrandBefore())
                                || boundaryStrands.contains(fn.getStrandAfter()))
                        .map(FullNotation::getCrossingId)
                        .collect(Collectors.toSet());

                boolean matchesAllCrossingTypes = boundaryType.crossings().stream()
                        .allMatch(crossingType ->
                                fnList.stream()
                                        .filter(fn -> boundaryCids.contains(fn.getCrossingId()))
                                        .anyMatch(fn ->
                                                (crossingType.sign() == null
                                                        || fn.getSign() == crossingType.sign())
                                                        && (crossingType.placement() == null
                                                        || fn.getPlacement() == crossingType.placement())));

                if (!matchesAllCrossingTypes) {
                    iterator.remove();
                }
            }
        }

        // Pair strands that run the same way around the boundary (both with it, or both against it).
        // Walking round a region, two such strands point in opposite directions across the plane,
        // which is what an oriented saddle joins; FullNotationMovesService.saddle reconnects them
        // as strand1 -> strand2's end and strand2 -> strand1's end, which stays planar. A strand
        // running with the boundary and one running against it are parallel in the plane, and
        // joining those that way makes the diagram non-planar.
        for (List<LongPair> boundary : orientedBoundaries) {
            for (long direction : new long[]{0L, 1L}) {
                List<Long> strands = boundary.stream()
                        .filter(pair -> pair.y() == direction)
                        .map(LongPair::x)
                        .distinct()
                        .sorted()
                        .toList();
                for (int i = 0; i < strands.size(); i++) {
                    for (int j = i + 1; j < strands.size(); j++) {
                        allPossibleSaddles.add(new LongPair(strands.get(i), strands.get(j)));
                    }
                }
            }
        }
        return allPossibleSaddles;
    }

    private HashMap<Long, Boolean> intersectStrandMaps(HashMap<Long, Boolean> current, HashMap<Long, Boolean> next) {
        if (current == null) {
            return new HashMap<>(next);
        }
        current.keySet().retainAll(next.keySet());
        return current;
    }

    // Strands that start (atStart = true) or end (atStart = false) at a crossing passage matching
    // crossingType. At each FullNotation row the strand leaving the passage is strandAfter and the
    // one arriving is strandBefore; a strand oriented against the diagram is read the other way.
    private HashMap<Long, Boolean> strandsAtCrossings(List<FullNotation> fnList, CrossingTypeDto crossingType, boolean atStart,
                                                      HashMap<Long, Boolean> orientedStrands) {
        HashMap<Long, Boolean> matching = new HashMap<>(orientedStrands);
        if (crossingType.sign() == null && crossingType.placement() == null) {
            return matching;
        }
        matching.clear();
        for (FullNotation fn : fnList) {
            boolean fits = (crossingType.sign() == null || crossingType.sign() == fn.getSign())
                    && (crossingType.placement() == null || crossingType.placement() == fn.getPlacement());
            if (!fits) {
                continue;
            }
            Long leaving = fn.getStrandAfter();
            Long arriving = fn.getStrandBefore();
            Long withDiagram = atStart ? leaving : arriving;
            Long against = atStart ? arriving : leaving;
            if (Boolean.TRUE.equals(orientedStrands.get(withDiagram))) {
                matching.put(withDiagram, true);
            }
            if (Boolean.FALSE.equals(orientedStrands.get(against))) {
                matching.put(against, false);
            }
        }
        return matching;
    }

    public HashMap<Long, Boolean> checkStrand(List<FullNotation> fnList, StrandTypeDto strandType, HashMap<Long, Boolean> orientedStrands) {

        if (strandType == null) {
            return new HashMap<>(orientedStrands);
        }
        HashMap<Long, Boolean> potentialStrands = null;
        if (strandType.orientedWith() != null) {
            HashMap<Long, Boolean> matchingStrands = new HashMap<>();
            for (Map.Entry<Long, Boolean> strand : orientedStrands.entrySet()) {
                if (strand.getValue().equals(strandType.orientedWith())) {
                    matchingStrands.put(strand.getKey(), strand.getValue());
                }
            }
            potentialStrands = intersectStrandMaps(potentialStrands, matchingStrands);
        }

        // crossing_before: the crossing the strand starts at; crossing_after: the one it ends at
        // (both in the strand's own direction, so orientedWith = false swaps them).
        if (strandType.crossing_before() != null) {
            potentialStrands = intersectStrandMaps(potentialStrands,
                    strandsAtCrossings(fnList, strandType.crossing_before(), true, orientedStrands));
        }
        if (strandType.crossing_after() != null) {
            potentialStrands = intersectStrandMaps(potentialStrands,
                    strandsAtCrossings(fnList, strandType.crossing_after(), false, orientedStrands));
        }
        return potentialStrands == null ? new HashMap<>(orientedStrands) : potentialStrands;
    }

    //Might be worth writing a way of doing this that goes through strands and returns the first one that hits all of the non-null specifications,
        //but doing so was yielding something not much computationally shorter, so to be revisited
    public Long identifyOneStrand(List<FullNotation> fnList, StrandTypeDto strandType, HashMap<Long, Boolean> orientedStrands) {
        HashMap<Long, Boolean> allStrands = checkStrand(fnList, strandType, orientedStrands);
        return allStrands.isEmpty() ? null : allStrands.keySet().iterator().next();
    }

    public LongPair identifyOneSaddle(List<FullNotation> fnList, BoundaryTypeDto boundaryType, HashMap<Long, Boolean> orientedStrands, List<List<LongPair>> orientedBoundaries) {
        List<LongPair> allSaddles = identifyAllPossibleSaddles(fnList, boundaryType, orientedStrands, orientedBoundaries);
        return allSaddles.isEmpty() ? null : allSaddles.get(0);
    }

    // Every triangle that can be slid (from FullNotationMovesService.slideableList), keeping only
    // proper triangles and those matching the optional SlideLoc filters.
    public List<List<Long>> identifyAllPossibleSlides(List<FullNotation> fnList, SlideLoc slideLoc, HashMap<Long, Boolean> orientedStrands) {
        List<List<Long>> boundaries = fullNotationMovesService.getOrientedBoundaries(fnList).stream()
                .map(boundary -> boundary.stream().map(LongPair::x).toList())
                .toList();
        Set<Set<Long>> seen = new HashSet<>();
        List<List<Long>> slides = new ArrayList<>();
        for (List<Long> triangle : fullNotationMovesService.slideableList(fnList, boundaries)) {
            if (!fullNotationMovesService.isSlideableTriangle(fnList, triangle) || !seen.add(new HashSet<>(triangle))) {
                continue;
            }
            if (slideLoc != null && slideLoc.strand() != null) {
                Set<Long> matching = checkStrand(fnList, slideLoc.strand(), orientedStrands).keySet();
                if (triangle.stream().noneMatch(matching::contains)) {
                    continue;
                }
            }
            CrossingTypeDto crossingType = slideLoc == null ? null : slideLoc.crossing();
            if (crossingType != null && (crossingType.sign() != null || crossingType.placement() != null)) {
                Set<Long> triangleCids = fnList.stream()
                        .filter(fn -> triangle.contains(fn.getStrandBefore()) || triangle.contains(fn.getStrandAfter()))
                        .map(FullNotation::getCrossingId)
                        .collect(Collectors.toSet());
                List<Long> matching = identifyAllCids(fnList, crossingType.sign(), crossingType.placement());
                if (matching.stream().noneMatch(triangleCids::contains)) {
                    continue;
                }
            }
            slides.add(triangle);
        }
        return slides;
    }

    public MoveRequestArgs convertRawRequestArgsSingle(List<FullNotation> fnList, MoveRequest<?, ?> req, HashMap<Long, Boolean> orientedStrands) {
        return convertRawRequestArgsAll(fnList, req, orientedStrands).stream().findFirst().orElse(null);
    }

    public List<MoveRequestArgs> convertRawRequestArgsAll(List<FullNotation> fnList, MoveRequest<?, ?> req, HashMap<Long, Boolean> orientedStrands) {
        List<MoveRequestArgs> result = new ArrayList<>();
        if (req instanceof NoArgsRequest && (req.type() == com.knots.backend.models.dtos.MoveType.mirror || req.type() == com.knots.backend.models.dtos.MoveType.flip)) {
            result.add(new MoveRequestArgs());
        } else if (req instanceof NoArgsRequest && req.type() == com.knots.backend.models.dtos.MoveType.remPokesAndTwists) {
            // Only fits when there is something to remove, so a run can tell when it has stopped changing.
            if (!fullNotationMovesService.getAllTwisted(fnList).isEmpty() || !fullNotationMovesService.getAllPoked(fnList).isEmpty()) {
                result.add(new MoveRequestArgs());
            }
        } else if (req instanceof RemTwistRequest twistReq) {
            Sign sign = twistReq.location() == null ? null : twistReq.location().sign();
            for (Long cid : fullNotationMovesService.getAllTwisted(fnList)) {
                boolean signMatches = sign == null || fnList.stream()
                        .anyMatch(fn -> fn.getCrossingId().equals(cid) && fn.getSign() == sign);
                if (signMatches) {
                    MoveRequestArgs args = new MoveRequestArgs();
                    args.setCid(cid);
                    result.add(args);
                }
            }
        } else if (req instanceof SwitchPlacementRequest switchReq) {
            Sign sign = switchReq.location() == null ? null : switchReq.location().sign();
            // identifyAllCids finds nothing without a filter, so "any crossing" lists them all.
            Set<Long> cids = new TreeSet<>(sign == null
                    ? fnList.stream().map(FullNotation::getCrossingId).toList()
                    : identifyAllCids(fnList, sign, null));
            for (Long cid : cids) {
                MoveRequestArgs args = new MoveRequestArgs();
                args.setCid(cid);
                result.add(args);
            }
        } else if (req instanceof RemPokeRequest) {
            for (LongPair poke : fullNotationMovesService.getAllPoked(fnList)) {
                try {
                    // In a link, some pokes can't be removed without leaving a component with no crossings.
                    fullNotationMovesService.remPoke(fnList, poke.x(), poke.y());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                MoveRequestArgs args = new MoveRequestArgs();
                args.setCid(poke.x());
                args.setCid2(poke.y());
                result.add(args);
            }
        } else if (req instanceof AddTwistRequest twistReq) {
            for (Long strand : new TreeSet<>(checkStrand(fnList, twistReq.location(), orientedStrands).keySet())) {
                MoveRequestArgs args = new MoveRequestArgs();
                args.setStrand(strand);
                args.setSign(twistReq.typeSpecificChoice());
                result.add(args);
            }
        } else if (!fnList.isEmpty() && req instanceof AddPokeRequest pokeReq) {
            Set<List<LongPair>> seen = new HashSet<>();
            for (List<LongPair> boundary : fullNotationMovesService.getOrientedBoundaries(fnList)) {
                for (LongPair strands : identifyAllPossiblePokes(fnList, pokeReq.location(), orientedStrands, List.of(boundary))) {
                    // strand1 goes under strand2; directions are relative to their shared face.
                    for (LongPair under : boundary) {
                        for (LongPair over : boundary) {
                            if (under.x().equals(strands.x()) && over.x().equals(strands.y())
                                    && seen.add(List.of(under, over))) {
                                MoveRequestArgs args = new MoveRequestArgs();
                                args.setUnderStrandOriented(under);
                                args.setOverStrandOriented(over);
                                result.add(args);
                            }
                        }
                    }
                }
            }
        } else if (!fnList.isEmpty() && req instanceof SlideRequest slideReq) {
            for (List<Long> triangle : identifyAllPossibleSlides(fnList, slideReq.location(), orientedStrands)) {
                MoveRequestArgs args = new MoveRequestArgs();
                args.setTriangleBdy(triangle);
                result.add(args);
            }
        } else if (!fnList.isEmpty() && req instanceof SaddleRequest saddleReq) {
            Set<LongPair> seen = new HashSet<>();
            for (LongPair strands : identifyAllPossibleSaddles(fnList, saddleReq.location(), orientedStrands, fullNotationMovesService.getOrientedBoundaries(fnList))) {
                LongPair unordered = new LongPair(Math.min(strands.x(), strands.y()), Math.max(strands.x(), strands.y()));
                if (seen.add(unordered)) {
                    MoveRequestArgs args = new MoveRequestArgs();
                    args.setStrand1(strands.x());
                    args.setStrand2(strands.y());
                    result.add(args);
                }
            }
        }
        return result;
    }

}
