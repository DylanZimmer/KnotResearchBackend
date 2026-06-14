package com.knots.backend.services;

import com.knots.backend.models.dtos.GeometricLine;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.GeometryDto;
import com.knots.backend.models.entities.*;
import com.knots.backend.repositories.VerticesAndArrowsRolfRepo;
import com.knots.backend.repositories.VerticesAndArrowsRepo;
import com.knots.backend.repositories.CrossingSpecsRolfRepo;
import com.knots.backend.repositories.CrossingSpecsRepo;
import com.knots.backend.repositories.DiagramsRolfRepo;

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

    public GeometryDto getGeometryByDiagramId(Long diagramId) {

        List<VerticesAndArrowsRolf> vs_and_as =
                verticesAndArrowsRolfRepo.findAllByDiagramIdOrderByStartPointAsc(diagramId);

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
                            va.getStartPoint(),
                            va.getEndPoint()
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
                            va.getStartPoint(),
                            va.getEndPoint()
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
                verticesAndArrowsRolfRepo.findAllByDiagramIdOrderByStartPointAsc(diagramId);

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
            copy.setStartPoint(va.getStartPoint());
            copy.setEndPoint(va.getEndPoint());
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
            Long temp = va.getStartPoint();
            va.setStartPoint(va.getEndPoint());
            va.setEndPoint(temp);
        }
    }

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

    private boolean redoNeeded(List<Long> remainingCids, List<Long> walk) {
        for (Long cid : remainingCids) {
            CrossingSpecs cspec = crossingSpecsRepo.findByCrossingId(cid);
            long under = cspec.getUnderLine();
            long over = cspec.getOverLine();
            if (walk.contains(under) && walk.contains(over)) {
                return true;
            }
        }
        return false;
    }

    //Assume cid1 is the crossing the polyline is coming from based on the orientation
    //crossing_specs has cid1, placement1, cid2, placement2
    //This gives me the arrows that the line is between
    //vertices_and_arrows has the walk, need to return an array with the walk
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
            long segment = walk.get(0);
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

}
