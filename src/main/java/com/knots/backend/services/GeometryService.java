package com.knots.backend.services;

import com.knots.backend.models.dtos.*;
import com.knots.backend.models.entities.*;
import com.knots.backend.repositories.*;

import jakarta.transaction.Transactional;
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




    //This will break if a crossing is on a bend. Irrelevant for now but potentially a thing later
    public Long checkSegmentForCrossing(LongPair corner1, LongPair corner2, Map<Long, LongPair> allCidCoords) {
        for (Map.Entry<Long, LongPair> crossings : allCidCoords.entrySet()) {
            LongPair thisCidCoords = crossings.getValue();
            if (corner1.x().equals(corner2.x())) {
                if (corner1.x().equals(thisCidCoords.x())) {
                    if ((corner1.y() < thisCidCoords.y() && thisCidCoords.y() < corner2.y())
                    || (corner2.y() < thisCidCoords.y() && thisCidCoords.y() < corner1.y())) {
                        System.out.println("Hit equal x's   :  " + corner1 + "    " + corner2 + "     " + thisCidCoords);
                        return crossings.getKey();
                    }
                }
            } else if (corner1.y().equals(corner2.y())) {
                if (corner1.y().equals(thisCidCoords.y())) {
                    if ((corner1.x() < thisCidCoords.x() && thisCidCoords.x() < corner2.x())
                    || (corner2.x() < thisCidCoords.x() && thisCidCoords.x() < corner1.x())) {
                        System.out.println("Hit equal ys   :  " + corner1 + "    " + corner2 + "     " + thisCidCoords);
                        return crossings.getKey();
                    }
                }
            }
        }
        return null;
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

    public Long walkToNextCid(Long point, Map<Long, LongPair> bendCoords, Map<Long, LongPair> allCidCoords, String handedness) {
        Long nextCid = null;
        Long firstPoint = point;
        while (nextCid == null) {
            LongPair firstCoords = bendCoords.get(firstPoint);
            Long secondPoint = getNextPoint(firstPoint, bendCoords.keySet(), handedness);
            LongPair secondCoords = bendCoords.get(secondPoint);
            nextCid = checkSegmentForCrossing(firstCoords, secondCoords, allCidCoords);
            System.out.println("************************");
            System.out.println(nextCid);
            System.out.println(firstPoint + " : " + firstCoords + "     " + secondPoint + " : " + secondCoords);
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&");
            if (nextCid == null) {
                firstPoint = secondPoint;
            }
        }
        return nextCid;
    }


    public cidDirecCid.Direction getStartingDirec(LongPair c1, LongPair c2) {
        if (c1.x().equals(c2.x())) {
            if (c1.y() < c2.y()) {
                return cidDirecCid.Direction.U;
            } else if (c1.y() > c2.y()) {
                return cidDirecCid.Direction.D;
            }
        } else if (c1.y().equals(c2.y())) {
            if (c1.x() < c2.x()) {
                return cidDirecCid.Direction.R;
            } else if (c1.x() > c2.x()) {
                return cidDirecCid.Direction.L;
            }
        }
        return null;
    }

    public cidDirecCid getCidDirecCid(Long cid, Long point, LongPair cidCoords, LongPair corner1, LongPair corner2, Map<Long, LongPair> allCidCoords, Map<Long, LongPair> bendCoords, String handedness) {
        Long nextCid = null;
        cidDirecCid.Direction direc = null;

        if (handedness.equals("R")) {
            nextCid = checkSegmentForCrossing(cidCoords, corner2, allCidCoords);
            direc = getStartingDirec(cidCoords, corner2);
            if (nextCid != null) {
                return new cidDirecCid (cid, direc, nextCid);
            }
        } else {
            nextCid = checkSegmentForCrossing(cidCoords, corner1, allCidCoords);
            direc = getStartingDirec(cidCoords, corner1);
            if (nextCid != null) {
                return new cidDirecCid (cid, direc, nextCid);
            }
        }

        Long nextPoint = getNextPoint(point, bendCoords.keySet(), handedness);
        nextCid = walkToNextCid(nextPoint, bendCoords, allCidCoords, handedness);
        return new cidDirecCid (cid, direc, nextCid);
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
        TwoXYPairs seg1 = orderCoords(verticesAndArrowsRepo.getCoordFromPt(crossLines.x()), verticesAndArrowsRepo.getCoordFromPt(getNextPoint(crossLines.x(), bendCoords.keySet(), "R")));
        TwoXYPairs seg2 = orderCoords(verticesAndArrowsRepo.getCoordFromPt(crossLines.y()), verticesAndArrowsRepo.getCoordFromPt(getNextPoint(crossLines.y(), bendCoords.keySet(), "R")));
        //The handedness shortcut isn't working. For 1 R on the trefoil I'm hitting it last, and the points should be going down but the handedness is R
        //I think I should go in and make each function return each side's cDC
        return new Walk (List.of (getCidDirecCid(cid, crossLines.x(), crossCoords, new LongPair(seg1.x1(), seg1.y1()), new LongPair(seg1.x2(), seg1.y2()), cidCoords, bendCoords, "L"),
        getCidDirecCid(cid, crossLines.x(), crossCoords, new LongPair(seg1.x1(), seg1.y1()), new LongPair(seg1.x2(), seg1.y2()), cidCoords, bendCoords, "R"),
        getCidDirecCid(cid, crossLines.y(), crossCoords, new LongPair(seg2.x1(), seg2.y1()), new LongPair(seg2.x2(), seg2.y2()), cidCoords, bendCoords, "L"),
        getCidDirecCid(cid, crossLines.y(), crossCoords, new LongPair(seg2.x1(), seg2.y1()), new LongPair(seg2.x2(), seg2.y2()), cidCoords, bendCoords, "R") ));
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



    /*
    public Walk remUsedCdcs(List<List<cidDirecCid>> boundaries, Walk cDirecCs) {
        Walk newCDirecCs = null;

        return newCDirecCs;
    }
    */

    /*
    public void createBoundariesFromWalks(Map<Long, Walk> cdcPerCid) {
        //walks looks like { 0 : { (0, U, cidx1), ... (0, L, cidx4) }
        //Each cdc should be in two boundaries, except for maybe twists? But as a base I'm
            //Free to put in that rule
        //Because the point is to go through every cDirecC from each cid and I can skip
            //The cDirecC's that are already in two dimensions

        List<List<cidDirecCid>> boundaries = null;

        for (Map.Entry<Long, Walk> entry : cdcPerCid.entrySet()) {
            Long cid = entry.getKey();
            Walk cDirecCs = entry.getValue();
            cDirecCs = remUsedCdcs(boundaries, cDirecCs);
            for (cidDirecCid cDc : cDirecCs) {

            }
            // use cid and walk
        }) {

        }
    }
    */

}
