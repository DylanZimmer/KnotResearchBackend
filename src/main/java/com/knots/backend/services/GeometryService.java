package com.knots.backend.services;

import com.knots.backend.models.dtos.*;
import com.knots.backend.models.entities.*;
import com.knots.backend.repositories.*;

import jakarta.transaction.Transactional;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import java.util.*;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeometryService {

    private final VerticesAndArrowsRolfRepo verticesAndArrowsRolfRepo;
    private final VerticesAndArrowsRepo verticesAndArrowsRepo;
    private final CrossingSpecsRolfRepo crossingSpecsRolfRepo;
    private final CrossingSpecsRepo crossingSpecsRepo;
    private final DiagramsRolfRepo diagramsRolfRepo;
    private final CurrentDiagramRepo currentDiagramRepo;


    public GeometryDto getGeometryByDiagramId(Long diagramId) {

        List<VerticesAndArrowsRolf> vs_and_as =
                verticesAndArrowsRolfRepo.findAllByDiagramIdOrderByPointAsc(diagramId);

        List<CrossingSpecsRolf> c_specs =
                crossingSpecsRolfRepo.findAllByDiagramIdOrderByCrossingIdAsc(diagramId);

        List<List<Long>> vertexPositionsList = new ArrayList<>();
        List<List<Long>> arrowsList = new ArrayList<>();
        List<List<Long>> crossingSpecsList = new ArrayList<>();

        for (CrossingSpecsRolf c : c_specs) {
            crossingSpecsList.add(
                    List.of(
                            c.getCrossingId(),
                            c.getUnderLine(),
                            c.getOverLine()
                    )
            );
        }
        for (VerticesAndArrowsRolf va : vs_and_as) {
            vertexPositionsList.add(
                    List.of(
                            va.getStrandX(),
                            va.getStrandY()
                    )
            );
            arrowsList.add(
                    List.of(
                            va.getPoint(),
                            va.getPoint()
                    )
            );
        }

        return new GeometryDto(
                vertexPositionsList,
                arrowsList,
                crossingSpecsList
        );
    }

    public GeometryDto getCurrentGeometry() {
        List<VerticesAndArrows> vs_and_as =
                verticesAndArrowsRepo.findAll();

        List<CrossingSpecs> c_specs =
                crossingSpecsRepo.findAll();

        List<List<Long>> vertexPositionsList = new ArrayList<>();
        List<List<Long>> arrowsList = new ArrayList<>();
        List<List<Long>> crossingSpecsList = new ArrayList<>();

        for (CrossingSpecs c : c_specs) {
            crossingSpecsList.add(
                    Arrays.asList(
                            c.getCrossingId(),
                            c.getUnderLine(),
                            c.getOverLine(),
                            c.getCrossingX(),
                            c.getCrossingY()
                    )
            );
        }
        for (VerticesAndArrows va : vs_and_as) {
            vertexPositionsList.add(
                    List.of(
                            va.getStrandX(),
                            va.getStrandY()
                    )
            );
            arrowsList.add(
                    List.of(
                            va.getPoint(),
                            va.getPoint()
                    )
            );
        }

        return new GeometryDto(
                vertexPositionsList,
                arrowsList,
                crossingSpecsList
        );
    }

    public long getDiagramIdByKnotId(Long knotId) {
        return diagramsRolfRepo.getDiagramIdByKnotId(knotId);
    }

    @Transactional
    public void clearCurrentGeometry() {
        verticesAndArrowsRepo.deleteAll();
        crossingSpecsRepo.deleteAll();
    }

    @Transactional
    public void copyGeometryByDiagramId(Long diagramId) {

        List<VerticesAndArrowsRolf> vs_and_as =
                verticesAndArrowsRolfRepo.findAllByDiagramIdOrderByPointAsc(diagramId);

        List<CrossingSpecsRolf> c_specs =
                crossingSpecsRolfRepo.findAllByDiagramIdOrderByCrossingIdAsc(diagramId);

        List<VerticesAndArrows> vs_and_as_copy = new ArrayList<>();
        List<CrossingSpecs> c_specs_copy = new ArrayList<>();

        for (VerticesAndArrowsRolf va : vs_and_as) {
            VerticesAndArrows copy = new VerticesAndArrows();
            copy.setDiagramId(va.getDiagramId());
            copy.setExtension(0L);
            copy.setStrandX(va.getStrandX());
            copy.setStrandY(va.getStrandY());
            copy.setPoint(va.getPoint());
            copy.setHandedness(va.getHandedness());
            vs_and_as_copy.add(copy);
        }

        for (CrossingSpecsRolf c : c_specs) {
            CrossingSpecs copy = new CrossingSpecs();
            copy.setDiagramId(c.getDiagramId());
            copy.setExtension(0L);
            copy.setCrossingId(c.getCrossingId());
            copy.setUnderLine(c.getUnderLine());
            copy.setOverLine(c.getOverLine());
            copy.setCrossingX(c.getCrossingX());
            copy.setCrossingY(c.getCrossingY());
            c_specs_copy.add(copy);
        }
        verticesAndArrowsRepo.saveAll(vs_and_as_copy);
        crossingSpecsRepo.saveAll(c_specs_copy);
    }

    @Transactional
    public void performMirror() {
        List<CrossingSpecs> c_specs = crossingSpecsRepo.findAll();
        for (CrossingSpecs c : c_specs) {
            Long temp = c.getUnderLine();
            c.setUnderLine(c.getOverLine());
            c.setOverLine(temp);
        }
    }

    @Transactional
    public void performOrientationFlip() {
        List<VerticesAndArrows> vs_and_as = verticesAndArrowsRepo.findAll();
        for (VerticesAndArrows va : vs_and_as) {
            if (va.getHandedness().equals("R")) {
                va.setHandedness("L");
            } else if (va.getHandedness().equals("L")) {
                va.setHandedness("R");
            }
        }
    }




    //************************************************************************************************

    public void addCSpecCorner(Long x, Long y, List<Pair<LongPair, LongPair>> segs) {
        Pair<LongPair, LongPair> toSplit = null;
        for (Pair<LongPair, LongPair> seg : segs) {
            if (seg.getFirst().x().equals(seg.getSecond().x()) && seg.getFirst().x().equals(x)) {
                toSplit = seg;
                break;
            }
            if (seg.getFirst().y().equals(seg.getSecond().y()) && seg.getFirst().y().equals(y)) {
                toSplit = seg;
                break;
            }
        }
        segs.remove(toSplit);
        segs.add(Pair.of(new LongPair(toSplit.getFirst().x(), toSplit.getFirst().y()), new LongPair(x, y)));
        segs.add(Pair.of(new LongPair(x, y), new LongPair(toSplit.getSecond().x(), toSplit.getSecond().y())));
    }

    public List<Pair<LongPair, LongPair>> organizeSegs(List<Pair<LongPair, LongPair>> segs) {
        List<Pair<LongPair, LongPair>> organizedSegs = new ArrayList<>();
        for (Pair<LongPair, LongPair> seg : segs) {
            if (seg.getFirst().x() > seg.getSecond().x() || seg.getFirst().y() > seg.getSecond().y()) {
                organizedSegs.add(Pair.of(seg.getSecond(), seg.getFirst()));
            } else {
                organizedSegs.add(seg);
            }
        }
        return organizedSegs;
    }

    //New idea here to create the boundaries through looking at the sweep of the longest remaining segments
    //Might be better to return a map instead of a list
    public List<Pair<LongPair, LongPair>> getSegments() {
        List<Pair<LongPair, LongPair>> segs = new ArrayList<>();
        List<CrossingSpecs> c_specs = crossingSpecsRepo.findAll();
        List<VerticesAndArrows> vs_and_as = verticesAndArrowsRepo.findAll();
        for (VerticesAndArrows va : vs_and_as) {
            segs.add(Pair.of(new LongPair (va.getStrandX(), va.getStrandY()), verticesAndArrowsRepo.getNextCoords(va.getPoint())));
        }
        for (CrossingSpecs c : c_specs) {
            addCSpecCorner(c.getCrossingX(), c.getCrossingY(), segs);
        }
        return organizeSegs(segs);
    }

    public Pair<LongPair, LongPair> longestSeg(List<Pair<LongPair, LongPair>> segs) {
        Long max = 0L;
        Pair<LongPair, LongPair> longestSeg = null;
        for (Pair<LongPair, LongPair> seg : segs) {
            Long dist = Math.abs(seg.getFirst().x() - seg.getSecond().x()) + Math.abs(seg.getFirst().y() - seg.getSecond().y());
            if (dist > max) {
                longestSeg = seg;
                max = dist;
            }
        }
        return longestSeg;
    }

    public boolean isVertical(Pair<LongPair, LongPair> seg) {
        if (seg.getFirst().x().equals(seg.getSecond().x())) {
            return true;
        } else {
            return false;
        }
    }

    public List<List<Pair<LongPair, LongPair>>> sweepSeg(Pair<LongPair, LongPair> sweepSeg, List<Pair<LongPair, LongPair>> segs) {
        List<List<Pair<LongPair, LongPair>>> inSweep = new ArrayList<>();
        inSweep.add(new ArrayList<>());
        inSweep.add(new ArrayList<>());
        if (isVertical(sweepSeg)) {
            for (Pair<LongPair, LongPair> seg : segs) {
                if (sweepSeg.getFirst().y() <= seg.getFirst().y() && seg.getSecond().y() <= sweepSeg.getSecond().y()) {
                    if (seg.getFirst().x() >= sweepSeg.getFirst().x()) { //On the seg, should be true for first or second. Equal on sweepSeg
                        inSweep.get(0).add(seg);
                    } else {
                        inSweep.get(1).add(seg);
                    }
                }
            }
        } else {
            for (Pair<LongPair, LongPair> seg : segs) {
                if (sweepSeg.getFirst().x() <= seg.getFirst().x() && seg.getSecond().x() <= sweepSeg.getSecond().x()) {
                    if (seg.getFirst().y() >= sweepSeg.getFirst().y()) { //On the seg, should be true for first or second. Equal on sweepSeg
                        inSweep.get(0).add(seg);
                    } else {
                        inSweep.get(1).add(seg);
                    }
                }
            }
        }
        return inSweep;
    }

    public Pair<LongPair, LongPair> getClosestSeg(Pair<LongPair, LongPair> seg, List<Pair<LongPair, LongPair>> partialBoundary) {
        Pair<LongPair, LongPair> nextSeg = null;
        Long minDist = 10000L;
        if (isVertical(seg)) {
            for (Pair<LongPair, LongPair> segInBoundary : partialBoundary) {
                Long dist = Math.abs(seg.getFirst().x() - segInBoundary.getFirst().x());
                if (dist < minDist && dist != 0L) {
                    nextSeg = segInBoundary;
                    minDist = dist;
                }
            }
        } else {
            for (Pair<LongPair, LongPair> segInBoundary : partialBoundary) {
                Long dist = Math.abs(seg.getFirst().y() - segInBoundary.getFirst().y());
                if (dist < minDist && dist != 0L) {
                    nextSeg = segInBoundary;
                    minDist = dist;
                }
            }
        }
        return nextSeg;
    }


    //I need to check if this is in the remainingSegs. If it's not then there's a line between them,
    public List<Pair<LongPair, LongPair>> connectingSeg_s(Pair<LongPair, LongPair> seg1, Pair<LongPair, LongPair> seg2) {
        List<Pair<LongPair, LongPair>> connectingSeg_s = new ArrayList<>();
        //Will either return one connecting seg or two if seg1 and seg2 perfectly match
        if (isVertical(seg1)) {
            //assert isVertical(seg2);
            if (seg1.getFirst().y().equals(seg2.getFirst().y())) {
                connectingSeg_s.add(Pair.of(seg1.getFirst(), seg2.getFirst()));
            }
            if (seg1.getSecond().y().equals(seg2.getSecond().y())) {
                connectingSeg_s.add(Pair.of(seg1.getSecond(), seg2.getSecond()));
            }
        } else {
            //assert !isVertical(seg2);
            if (seg1.getFirst().x().equals(seg2.getFirst().x())) {
                connectingSeg_s.add(Pair.of(seg1.getFirst(), seg2.getFirst()));
            }
            if (seg1.getSecond().x().equals(seg2.getSecond().x())) {
                connectingSeg_s.add(Pair.of(seg1.getSecond(), seg2.getSecond()));
            }
        }
        return connectingSeg_s;
    }

    public List<List<Pair<LongPair, LongPair>>> completeBoundaryFromSweep(List<List<Pair<LongPair, LongPair>>> partialBoundariesFromSweep, List<Pair<LongPair, LongPair>> remainingSegs) {
        List<List<Pair<LongPair, LongPair>>> fullBoundaries = partialBoundariesFromSweep;
        //This will contain either all vertical or all horizontal. Then I need to connect them
        while (!partialBoundariesFromSweep.isEmpty()) {
            for (List<Pair<LongPair, LongPair>> boundary : partialBoundariesFromSweep) {
                while (boundary.size() > 1) {
                    Pair<LongPair, LongPair> nextSeg = boundary.get(0);
                    Pair<LongPair, LongPair> seg2 = getClosestSeg(nextSeg, boundary);
                    List<Pair<LongPair, LongPair>> connectingSeg_s = connectingSeg_s(nextSeg, seg2);
                    fullBoundaries.boundary.addAll(connectingSeg_s);
                }
                if (!alreadyConnected(boundary.get(0), fullBoundaries.boundary)) {

                }
                partialBoundariesFromSweep.remove(boundary);
            }
        }

        for (List<Pair<LongPair, LongPair>> boundary : partialBoundaries) {
            for (Pair<LongPair, LongPair> seg : boundary) {
                Pair<LongPair, LongPair> seg2 = getClosestSeg(seg, boundary);
                List<Pair<LongPair, LongPair>> connectingSeg_s = connectingSeg_s(seg, seg2);
                for (Pair<LongPair, LongPair> seg_s : connectingSeg_s) {

                }
            }
        }
        return newBoundaries;
    }

    public List<Pair<LongPair, LongPair>> remUsedSegs(List<Pair<LongPair, LongPair>> segs, List<List<Pair<LongPair, LongPair>>> boundaries) {
        List<Pair<LongPair, LongPair>> remSegs = new ArrayList<>();
        segLoop:
        for (Pair<LongPair, LongPair> seg : segs) {
            int cnt = 0;
            for (List<Pair<LongPair, LongPair>> boundary : boundaries) {
                for (Pair<LongPair, LongPair> boundedSeg : boundary) {
                    if (boundedSeg.equals(seg)) {
                        cnt ++;
                        if (cnt == 2) {
                            continue segLoop;
                        }
                    }
                }
            }
            remSegs.add(seg);
        }
        return remSegs;
    }

    public List<List<Pair<LongPair, LongPair>>> getBoundaries() {
        List<Pair<LongPair, LongPair>> remainingSegs = getSegments();
        List<List<Pair<LongPair, LongPair>>> boundaries = new ArrayList<>();
        //This will probably be a while loop until the boundaries are done or something
        //probably while boundaries isn't done, or segs still has segs
        while (!remainingSegs.isEmpty()) {
            Pair<LongPair, LongPair> longestSeg = longestSeg(remainingSegs);
            List<List<Pair<LongPair, LongPair>>> partialBoundaries = sweepSeg(longestSeg, remainingSegs);
            boundaries.addAll(completeBoundaryFromSweep(partialBoundaries, remainingSegs));
            remainingSegs = remUsedSegs(remainingSegs, boundaries);
        }
        return boundaries;
    }






    /*
    public long getArrowFromCrossingSpecs(long cid, String placement) {
        CrossingSpecs cspecs = crossingSpecsRepo.findByCrossingId(cid);
        if (placement.equals("over")) {
            return cspecs.getOverLine();
        } else if (placement.equals("under")) {
            return cspecs.getUnderLine();
        }
        return 0;
    }

    private List<Long> getWalk(long arrow1, long arrow2) {
        long currStep = arrow1;
        List<Long> walk = new ArrayList<>();
        walk.add(currStep);
        while (currStep != arrow2) {
            currStep = verticesAndArrowsRepo.findEndPointByStartPoint(currStep);
            walk.add(currStep);
        }
        return walk;
    }
     */

    //WRONG WRONG WRONG
    /*
    private boolean redoNeeded(List<Long> remainingCids, List<Long> walk) {
        for (Long cid : remainingCids) {
            CrossingSpecs cspec = crossingSpecsRepo.findByCrossingId(cid);
            long under = cspec.getUnderLine();
            long over = cspec.getOverLine();

            if (walk.contains(under) && walk.contains(over)) {
                //I'm operating under the assumption that no redo is needed only if under
                    //is either first or last and over is the other one
                if (walk.indexOf(under) == 0 && walk.indexOf(over) == walk.size() - 1) {
                    return false;
                } else if (walk.indexOf(over) == 0 && walk.indexOf(under) == walk.size() - 1) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    */

    //Assume cid1 is the crossing the polyline is coming from based on the orientation
    //crossing_specs has cid1, placement1, cid2, placement2
    //This gives me the arrows that the line is between
    //vertices_and_arrows has the walk, need to return an array with the walk
    /*
    public List<Long> findWalkFromLine(GeometricLine line) {
        long arrow1 = getArrowFromCrossingSpecs(line.cid1(), line.placement1());
        long arrow2 = getArrowFromCrossingSpecs(line.cid2(), line.placement2());
        List<Long> remainingCids = crossingSpecsRepo.getRemainingCrossingIds(List.of(line.cid1(), line.cid2()));
        List<Long> walk = getWalk(arrow1, arrow2);
        boolean redoNeeded = redoNeeded(remainingCids, walk);
        if (!redoNeeded) {
            return walk;
        } else {
            walk = getWalk(arrow2, arrow1);
            if (!redoNeeded(remainingCids, walk)) {
                return walk;
            } else {
                return new ArrayList<>();
            }
        }
    }
    */

    /*
    public List<Long> findWalkFromLine(GeometricLine line) {
        long arrow1 = getArrowFromCrossingSpecs(line.cid1(), line.placement1());
        long arrow2 = getArrowFromCrossingSpecs(line.cid2(), line.placement2());
        List<Long> walk1 = getWalk(arrow1, arrow2);
        List<Long> walk2 = getWalk(arrow2, arrow1);
        if (walk1.size() < walk2.size()) {
            return walk1;
        } else {
            return walk2;
        }
    }

    public long mid(long x, long y) {
        return (x+y)/2;
    }

    //Between the crossings
    public LongPair twistCoordinatesOneSegment(Long cid1, Long cid2) {
        LongPair start = crossingSpecsRepo.findCrossingCoordinatesFromCrossingId(cid1);
        LongPair end = crossingSpecsRepo.findCrossingCoordinatesFromCrossingId(cid2);
        return new LongPair (mid(start.x(),end.x()), mid(start.y(), end.y()));
    }

    //public LongPair getArrowTwoSegments()

    //On the larger segment
    public long twistSegmentChoiceTwoSegments(List<Long> walk, Long cid1, Long cid2) {
        LongPair bend = verticesAndArrowsRepo.findStrandCoordinatesFromStartPoint(walk.get(1));
        LongPair crossingCoords1 = crossingSpecsRepo.findCrossingCoordinatesFromCrossingId(cid1);
        LongPair crossingCoords2 = crossingSpecsRepo.findCrossingCoordinatesFromCrossingId(cid2);
        Long dist1 = Math.abs(crossingCoords1.x() - bend.x()) + Math.abs(crossingCoords1.y() - bend.y());
        Long dist2 = Math.abs(crossingCoords2.x() - bend.x()) + Math.abs(crossingCoords2.y() - bend.y());
        if (dist1 >= dist2) {
            return cid1;
        } else {
            return cid2;
        }
    }

    public LongPair twistCoordinatesTwoSegments(List<Long> walk, Long cid) {
        LongPair bend = verticesAndArrowsRepo.findStrandCoordinatesFromStartPoint(walk.get(1));
        LongPair crossingCoords = crossingSpecsRepo.findCrossingCoordinatesFromCrossingId(cid);
        return new LongPair (mid(crossingCoords.x(), bend.x()), mid(crossingCoords.y(), bend.y()));
    }

    //On the first blank segment
    public LongPair twistCoordinatesThreePlusSegments(List<Long> walk) {
        LongPair start = verticesAndArrowsRepo.findStrandCoordinatesFromStartPoint(walk.get(1));
        LongPair end = verticesAndArrowsRepo.findStrandCoordinatesFromStartPoint(walk.get(2));
        return new LongPair (mid(start.x(),end.x()), mid(start.y(), end.y()));
    }

    //Take in geometricLine
    //Calculate arrows needed from vertices_and_arrows to get from one crossing to the other
    //Find LongPair to put twist
    //Add one row to crossing_specs
    @Transactional
    public void performTwist(GeometricLine line, String handedness) {
        List<Long> walk = findWalkFromLine(line);
        long arrow = 0;
        LongPair twistCoordinates = null;
        if (walk.size() == 1) {
            twistCoordinates = twistCoordinatesOneSegment(line.cid1(), line.cid2());
            arrow = walk.get(0);
        } else if (walk.size() == 2) {
            long cid = twistSegmentChoiceTwoSegments(walk, line.cid1(), line.cid2());
            twistCoordinates = twistCoordinatesTwoSegments(walk, cid);
            if (cid == line.cid1()) {
                arrow = walk.get(0);
            } else if (cid == line.cid2()) {
                arrow = walk.get(1);
            }
        } else if (walk.size() >= 3) {
            twistCoordinates = twistCoordinatesThreePlusSegments(walk);
            arrow = walk.get(1);
        }
        CrossingSpecs cspec = new CrossingSpecs();
        long maxCId = crossingSpecsRepo.getMaxCrossingId();
        cspec.setCrossingId(maxCId + 1);
        if (handedness.equals("r")) {
            cspec.setOverLine(arrow);
        } else if (handedness.equals("l")) {
            cspec.setUnderLine(arrow);
        }
        cspec.setCrossingX(twistCoordinates.x());
        cspec.setCrossingY(twistCoordinates.y());
        long maxCsId = crossingSpecsRepo.getMaxCsId();
        cspec.setCsId(maxCsId + 1);
        long currDiagramId = crossingSpecsRepo.getCurrDiagramId(0L);
        cspec.setDiagramId(currDiagramId);
        long currExtension = crossingSpecsRepo.getCurrExtension(0L);
        cspec.setExtension(currExtension);
        crossingSpecsRepo.save(cspec);
    }

     */




    //This tried to use handedness to go to the actual next cid, but that's unecessary, I need both




    /*
    //This will break if a crossing is on a bend. Irrelevant for now but potentially a thing later
    public Long checkSegmentForCrossing(LongPair corner1, LongPair corner2, Map<Long, LongPair> allCidCoords) {
        Long dist = null;
        Long nextCid = null;
        for (Map.Entry<Long, LongPair> crossings : allCidCoords.entrySet()) {
            Long newDist = null;
            LongPair thisCidCoords = crossings.getValue();
            if (corner1.x().equals(corner2.x())) {
                if (corner1.x().equals(thisCidCoords.x())) {
                    if ((corner1.y() < thisCidCoords.y() && thisCidCoords.y() < corner2.y())
                    || (corner2.y() < thisCidCoords.y() && thisCidCoords.y() < corner1.y())) {
                        newDist = Math.abs(thisCidCoords.y() - corner1.y());
                    }
                }
            } else if (corner1.y().equals(corner2.y())) {
                if (corner1.y().equals(thisCidCoords.y())) {
                    if ((corner1.x() < thisCidCoords.x() && thisCidCoords.x() < corner2.x())
                    || (corner2.x() < thisCidCoords.x() && thisCidCoords.x() < corner1.x())) {
                        newDist = Math.abs(thisCidCoords.x() - corner1.x());
                    }
                }
            }
            if (newDist != null) {
                if (dist == null || newDist < dist) {
                    dist = newDist;
                    nextCid = crossings.getKey();
                }
            }
        }
        return nextCid;
    }

    public Long getNextPoint(Long point, Collection<Long> allPoints, String handedness) {
        Long nextPoint = null;
        Long maxPoint = Collections.max(allPoints);
        if (handedness.equals("R")) {
            if (point.equals(maxPoint)) {
                nextPoint = 0L;
            } else {
                nextPoint = point + 1;
            }
        } else {
            if (point.equals(0L)) {
                nextPoint = maxPoint;
            } else {
                nextPoint = point - 1;
            }
        }
        return nextPoint;
    }

    public Pair<Long, DrawnLine.Direction> walkToNextCid(Long point, Map<Long, LongPair> bendCoords, Map<Long, LongPair> allCidCoords, String handedness) {
        Long nextCid = null;
        Long firstPoint = point;
        LongPair firstCoords = null;
        while (nextCid == null) {
            firstCoords = bendCoords.get(firstPoint);
            Long secondPoint = getNextPoint(firstPoint, bendCoords.keySet(), handedness);
            LongPair secondCoords = bendCoords.get(secondPoint);
            nextCid = checkSegmentForCrossing(firstCoords, secondCoords, allCidCoords);
            if (nextCid == null) {
                firstPoint = secondPoint;
            }
        }
        LongPair nextCidCoords = crossingSpecsRepo.findCrossingCoordinatesFromCrossingId(nextCid);
        DrawnLine.Direction dn = getDirection(firstCoords, nextCidCoords);
        return Pair.of(nextCid, dn);
    }


    public DrawnLine.Direction getDirection(LongPair c1, LongPair c2) {
        if (c1.x().equals(c2.x())) {
            if (c1.y() < c2.y()) {
                return DrawnLine.Direction.U;
            } else if (c1.y() > c2.y()) {
                return DrawnLine.Direction.D;
            }
        } else if (c1.y().equals(c2.y())) {
            if (c1.x() < c2.x()) {
                return DrawnLine.Direction.R;
            } else if (c1.x() > c2.x()) {
                return DrawnLine.Direction.L;
            }
        }
        return null;
    }

    public DrawnLine getDrawnLine(Long cid, Long point, LongPair cidCoords, LongPair corner1, LongPair corner2, Map<Long, LongPair> allCidCoords, Map<Long, LongPair> bendCoords, String handedness) {
        Long nextCid = null;
        DrawnLine.Direction dn1 = null;
        Long walkStart = null;
        if (handedness.equals("R")) {
            nextCid = checkSegmentForCrossing(cidCoords, corner2, allCidCoords);
            dn1 = getDirection(cidCoords, corner2);
            if (nextCid != null) {
                return new DrawnLine(cid, dn1, dn1, nextCid, DrawnLine.SameSeg.Y);
                //Double dn1 because directions are the same for one-segment lines
            }
            walkStart = getNextPoint(point, bendCoords.keySet(), handedness);
        } else {
            nextCid = checkSegmentForCrossing(cidCoords, corner1, allCidCoords);
            dn1 = getDirection(cidCoords, corner1);
            if (nextCid != null) {
                return new DrawnLine(cid, dn1, dn1, nextCid, DrawnLine.SameSeg.Y);
            }
            walkStart = point;
        }

        Pair<Long, DrawnLine.Direction> nextCidNDn = walkToNextCid(walkStart, bendCoords, allCidCoords, handedness);
        return new DrawnLine (cid, dn1, nextCidNDn.getSecond(), nextCidNDn.getFirst());
    }

    public TwoXYPairs orderCoords(LongPair seg1, LongPair seg2) {
        if (seg1.x().equals(seg2.x())) {
            if (seg1.y() < seg2.y()) {
                return new TwoXYPairs(seg1.x(), seg1.y(), seg2.x(), seg2.y());
            } else {
                return new TwoXYPairs(seg2.x(), seg2.y(), seg1.x(), seg1.y());
            }
        } else if (seg1.y().equals(seg2.y())) {
            if (seg1.x() < seg2.x()) {
                return new TwoXYPairs(seg1.x(), seg1.y(), seg2.x(), seg2.y());
            } else {
                return new TwoXYPairs(seg2.x(), seg2.y(), seg1.x(), seg1.y());
            }
        } else {
            return null;
        }
    }

    //Takes in cid, cidCrossLines, cidCoords, allCidCoords, allBends
    //Needs to output { (cid, Direc, cidx0), ... (cid, Direc, cidx3) }
    public Walk createEachDirection(Long cid, LongPair crossLines, LongPair crossCoords, Map<Long, LongPair> cidCoords, Map<Long, LongPair> bendCoords) {

        //TwoXYPairs seg1 = orderCoords(verticesAndArrowsRepo.getCoordFromPt(crossLines.x()), verticesAndArrowsRepo.getCoordFromPt(getNextPoint(crossLines.x(), bendCoords.keySet(), "R")));
        //TwoXYPairs seg2 = orderCoords(verticesAndArrowsRepo.getCoordFromPt(crossLines.y()), verticesAndArrowsRepo.getCoordFromPt(getNextPoint(crossLines.y(), bendCoords.keySet(), "R")));


        TwoXYPairs seg1 = verticesAndArrowsRepo.getSegFromCrossingLine(crossLines.x(), getNextPoint(crossLines.x(), bendCoords.keySet(), "R"));
        TwoXYPairs seg2 = verticesAndArrowsRepo.getSegFromCrossingLine(crossLines.y(), getNextPoint(crossLines.y(), bendCoords.keySet(), "R"));
        //The handedness shortcut isn't working. For 1 R on the trefoil I'm hitting it last, and the points should be going down but the handedness is R
        //I think I should go in and make each function return each side's cDC
        return new Walk (List.of (getDrawnLine(cid, crossLines.x(), crossCoords, new LongPair(seg1.x1(), seg1.y1()), new LongPair(seg1.x2(), seg1.y2()), cidCoords, bendCoords, "L"),
        getDrawnLine(cid, crossLines.x(), crossCoords, new LongPair(seg1.x1(), seg1.y1()), new LongPair(seg1.x2(), seg1.y2()), cidCoords, bendCoords, "R"),
        getDrawnLine(cid, crossLines.y(), crossCoords, new LongPair(seg2.x1(), seg2.y1()), new LongPair(seg2.x2(), seg2.y2()), cidCoords, bendCoords, "L"),
        getDrawnLine(cid, crossLines.y(), crossCoords, new LongPair(seg2.x1(), seg2.y1()), new LongPair(seg2.x2(), seg2.y2()), cidCoords, bendCoords, "R") ));
    };


    //This is gonna be a dict with
    // { cid : [ (id, direc, id), x4 ] ,... }
    //For each crossing, up is the next vertex that's at a corner, so it's along the walk
    //Each crossing shows which two lines it's on, doesn't matter which is over and which is under
    //Start with a crossing.
    public Map<Long, Walk> getSegmentsByCrossing() {
        Map<Long, Walk> segments = new HashMap<>();

        Map<Long, List<LongPair>> linesThenCoords = new HashMap<>();
        Map<Long, LongPair> allCidCoords = new HashMap<>();
        List<CrossingSpecs> cspecs = crossingSpecsRepo.findAll();
        for (CrossingSpecs cspec : cspecs) {
            Long cid = cspec.getCrossingId();
            LongPair cidCoords = new LongPair(cspec.getCrossingX(), cspec.getCrossingY());
            linesThenCoords.put(cspec.getCrossingId(), List.of(
                    new LongPair(cspec.getUnderLine(), cspec.getOverLine()),
                    cidCoords
            ));
            allCidCoords.put(cid, cidCoords);
        }

        Map<Long, LongPair> bendCoords = new HashMap<>();
        List<VerticesAndArrows> vsAndAs = verticesAndArrowsRepo.findAll();
        for (VerticesAndArrows nxtVsAndAs : vsAndAs) {
            bendCoords.put(nxtVsAndAs.getPoint(), new LongPair(nxtVsAndAs.getStrandX(), nxtVsAndAs.getStrandY()));
        }

        for (Map.Entry<Long, List<LongPair>> entry : linesThenCoords.entrySet()) {
            Long cid = entry.getKey();
            LongPair crossLines = entry.getValue().get(0);
            LongPair crossCoords = entry.getValue().get(1);
            segments.put(cid, createEachDirection(cid, crossLines, crossCoords, allCidCoords, bendCoords));
        }

        return segments;
    }


    public Pair<List<List<DrawnLine>>, Map<Walk, Walk>> checkLatestLinesForBoundaries(Map<Walk, Walk> nextLines) {
        List<List<DrawnLine>> tmpBoundaries = new ArrayList<>();
        for (Map.Entry<Walk, Walk> entry : nextLines.entrySet()) {
            Walk newEntries = entry.getValue();
            Walk prevEntries = entry.getKey();
            if (newEntries == null || prevEntries == null) {
                return Pair.of(tmpBoundaries, nextLines);
            }
            List<DrawnLine> prevSegments = prevEntries.segments();
            Iterator<DrawnLine> iterator = newEntries.segments().iterator();
            while (iterator.hasNext()) {
                DrawnLine newLine = iterator.next();
                for (int i = 0; i < prevSegments.size(); i++) {
                    if (prevSegments.get(i).cid1().equals(newLine.cid2())) {
                        List<DrawnLine> boundary = new ArrayList<>(prevSegments.subList(i, prevSegments.size()));
                        boundary.add(newLine);
                        tmpBoundaries.add(boundary);
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        return Pair.of(tmpBoundaries, nextLines);
    }

    public DrawnLine.Direction revDn(DrawnLine.Direction dn) {
        if (dn == null) {
            return null;
        }
        return switch (dn) {
            case U -> DrawnLine.Direction.D;
            case D -> DrawnLine.Direction.U;
            case R -> DrawnLine.Direction.L;
            case L -> DrawnLine.Direction.R;
        };
    }

    public boolean reverseLines(DrawnLine line1, DrawnLine line2) {
        if (line1.cid1().equals(line2.cid2()) &&
            line1.cid2().equals(line2.cid1()) &&
            Objects.equals(line1.dn1(), (revDn(line2.dn2()))) &&
            Objects.equals(line1.dn2(), revDn(line2.dn1()))) {
                return true;
        } else {
            return false;
        }
    }

    //public void createNextLinesForBoundary(Map<Walk, Walk> nextLines, Map<Long, Walk> segs) {
    public Map<Walk, Walk> createNextLinesForBoundary(Map<Walk, Walk> nextLines, Map<Long, Walk> segs) {
        Map<Walk, Walk> newNxt = new HashMap<>();
        for (Map.Entry<Walk, Walk> entry : nextLines.entrySet()) {
            Walk prevWalk = entry.getKey();
            Walk currentWalk = entry.getValue();
            for (DrawnLine line : currentWalk.segments()) {
                List<DrawnLine> extendedSegments = new ArrayList<>(prevWalk.segments());
                if (!reverseLines(prevWalk.segments().get(prevWalk.segments().size()-1), line)) {
                    extendedSegments.add(line);
                    Walk extendedWalk = new Walk(extendedSegments);
                    Walk nextWalk = segs.get(line.cid2());
                    newNxt.put(extendedWalk, nextWalk);
                }
            }
        }
        return newNxt;
    }

    public DrawnLine createReverseLine(DrawnLine line) {
        return new DrawnLine(line.cid2(), revDn(line.dn2()), revDn(line.dn1()), line.cid1(), line.sameSeg());
    }

    public Pair<DrawnLine, DrawnLine> smallerDLCidFirst(DrawnLine line1, DrawnLine line2) {
        if (line1.cid1() < line2.cid1()) {
            return Pair.of(line1, line2);
        } else {
            return Pair.of(line2, line1);
        }
    }

    public void updateLinesInBoundaries(Map<Pair<DrawnLine, DrawnLine>, Long> linesInBoundaries, List<List<DrawnLine>> newBoundaries) {
        for (List<DrawnLine> boundary : newBoundaries) {
            for (DrawnLine line : boundary) {
                linesInBoundaries.merge(smallerDLCidFirst(line, createReverseLine(line)), 1L, Long::sum);
            }
        }
    }

    public void removeUsedUpSegs(Map<Long, Walk> segs, Map<Pair<DrawnLine, DrawnLine>, Long> linesInBoundaries) {
        for (Map.Entry<Pair<DrawnLine, DrawnLine>, Long> entry : linesInBoundaries.entrySet()) {
            if (entry.getValue() == 2) {
                Pair<DrawnLine, DrawnLine> lines = entry.getKey();
                removeLineFromSegs(segs, lines.getFirst());
                removeLineFromSegs(segs, lines.getSecond());
            }
        }
    }

    private void removeLineFromSegs(Map<Long, Walk> segs, DrawnLine line) {
        Walk walk = segs.get(line.cid1());
        if (walk == null) return;
        List<DrawnLine> remaining = new ArrayList<>(walk.segments());
        remaining.remove(line);
        if (remaining.isEmpty()) {
            segs.remove(line.cid1());
        } else {
            segs.put(line.cid1(), new Walk(remaining));
        }
    }

    public Pair<List<List<DrawnLine>>, Map<Walk, Long>> stepBoundaries(Map<Walk, Long> nextLines, Map<Long, Walk> segs) {
        List<List<DrawnLine>> newBoundaries = new ArrayList<>();
        Map<Walk, Long> newNextLines = new HashMap<>();

        for (Map.Entry<Walk, Long> entry : nextLines.entrySet()) {
            Walk prevWalk = entry.getKey();
            Long cid = entry.getValue();
            Walk currentWalk = segs.get(cid);
            if (currentWalk == null) continue; // crossing already fully consumed; branch dies

            List<DrawnLine> prevSegments = prevWalk.segments();
            DrawnLine lastLine = prevSegments.get(prevSegments.size() - 1);

            for (DrawnLine line : currentWalk.segments()) {
                if (reverseLines(lastLine, line)) continue; // no immediate backtrack

                boolean closed = false;
                for (int i = 0; i < prevSegments.size(); i++) {
                    if (prevSegments.get(i).cid1().equals(line.cid2())) {
                        List<DrawnLine> boundary = new ArrayList<>(prevSegments.subList(i, prevSegments.size()));
                        boundary.add(line);
                        newBoundaries.add(boundary);
                        closed = true;
                        break;
                    }
                }
                if (!closed) {
                    List<DrawnLine> extended = new ArrayList<>(prevSegments);
                    extended.add(line);
                    newNextLines.put(new Walk(extended), line.cid2());
                }
            }
        }
        return Pair.of(newBoundaries, newNextLines);
    }

    public List<List<DrawnLine>> walkOnDrawnLinesForBoundaries(Map<Long, Walk> segs) {
        Map<Pair<DrawnLine, DrawnLine>, Long> linesInBoundaries = new HashMap<>();
        List<List<DrawnLine>> newBoundaries = new ArrayList<>();

        Map<Walk, Long> nextLines = new HashMap<>();
        DrawnLine seed = segs.values().iterator().next().segments().get(0);
        nextLines.put(new Walk(List.of(seed)), seed.cid2());

        while (!segs.isEmpty() && !nextLines.isEmpty()) {
            if (nextLines.isEmpty()) {
                DrawnLine remaining = segs.values().iterator().next().segments().get(0);
                nextLines.put(new Walk(List.of(remaining)), remaining.cid2());
            }
            System.out.println("***********************");
            System.out.println("segs   : " + segs);
            System.out.println("nextLines : " + nextLines);
            Pair<List<List<DrawnLine>>, Map<Walk, Long>> res = stepBoundaries(nextLines, segs);
            System.out.println("res : " + res);
            newBoundaries.addAll(res.getFirst());
            updateLinesInBoundaries(linesInBoundaries, res.getFirst());
            System.out.println("linesInBoundaries  : " + linesInBoundaries);
            removeUsedUpSegs(segs, linesInBoundaries);
            nextLines = res.getSecond();
            System.out.println("nextLines : " + nextLines);
        }
        return newBoundaries;
    }

    public List<List<DrawnLine>> getBoundaries() {
        Map<Long, Walk> segsFull = getSegmentsByCrossing();
        Map<Long, Walk> segs = new HashMap<>();
        List<List<DrawnLine>> boundaries = new ArrayList<>();
        //First need to record & collapse the 2-crossing boundaries
        //Then need to do a divergent walk from each segment
        for (Map.Entry<Long, Walk> cidEntry : segsFull.entrySet()) {
            Set<Long> seen = new HashSet<>();
            List<DrawnLine> newSegs = new ArrayList<>();
            for (DrawnLine line : cidEntry.getValue().segments()) {
                Long lCid2 = line.cid2();
                if (seen.add(lCid2)) {
                    newSegs.add(line);
                } else {
                    //The two crossing boundaries are coming through but I should format the second line to have the cid2 first, be the way back
                    boundaries.add(List.of(newSegs.stream().filter(seg -> seg.cid2().equals(lCid2)).findFirst().orElse(null), line));
                    newSegs.removeIf(seg -> seg.cid2().equals(lCid2));
                    newSegs.add(new DrawnLine(line.cid1(), null, null, lCid2));
                }
            }
            segs.put(cidEntry.getKey(), new Walk(newSegs));
        }
        boundaries.addAll(walkOnDrawnLinesForBoundaries(segs));
        return boundaries;
    }
     */
}
