package com.knots.backend.services;

import com.knots.backend.models.dtos.*;
import com.knots.backend.models.entities.*;
import com.knots.backend.repositories.*;

import jakarta.transaction.Transactional;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import java.util.*;
import lombok.RequiredArgsConstructor;

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

        String handedness = currentDiagramRepo.findFirstBy().getHandedness();

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
                crossingSpecsList,
                handedness
        );
    }

    public GeometryDto getCurrentGeometry() {
        List<VerticesAndArrows> vs_and_as =
                verticesAndArrowsRepo.findAll();

        List<CrossingSpecs> c_specs =
                crossingSpecsRepo.findAll();

        String handedness = currentDiagramRepo.findFirstBy().getHandedness();

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
                crossingSpecsList,
                handedness
        );
    }

    public long getDiagramIdByKnotId(Long knotId) {
        return diagramsRolfRepo.getDiagramIdByKnotId(knotId);
    }

    @Transactional
    public void clearCurrentGeometry() {
        verticesAndArrowsRepo.deleteAll();
        crossingSpecsRepo.deleteAll();
        currentDiagramRepo.deleteAll();
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

        CurrentDiagram currDiagram = new CurrentDiagram();

        currDiagram.setDiagramId(diagramId);
        currDiagram.setHandedness("R");
        currDiagram.setExtension(0L);

        verticesAndArrowsRepo.saveAll(vs_and_as_copy);
        crossingSpecsRepo.saveAll(c_specs_copy);
        currentDiagramRepo.save(currDiagram);
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
        CurrentDiagram currDiagram = currentDiagramRepo.findFirstBy();
        currDiagram.setHandedness(currDiagram.getHandedness().equals("R") ? "L" : "R");
    }

    //Below is the 'fill out rectangles' idea
    //Start with the most left-most line(s)
    //Find the upper and lower bounds for the rectangle that the left-most line is the left-most on
    //Define the right-side to start on the top or bottom segment that hits its next point first
    //Repeat the process until the right line equals a segment that actually exists

    public boolean isVertical(Pair<LongPair, LongPair> seg) {
        if (seg.getFirst().x().equals(seg.getSecond().x())) {
            return true;
        } else {
            return false;
        }
    }

    private Boolean pointInSeg(LongPair pt, Pair<LongPair, LongPair> seg) {
        if (seg.getFirst().x().equals(seg.getSecond().x()) && seg.getFirst().x().equals(pt.x())) {
            if (seg.getFirst().y() <= pt.y() && pt.y() <= seg.getSecond().y()
                    || seg.getSecond().y() <= pt.y() && pt.y() <= seg.getFirst().y()) {
                return true;
            }
        } else if (seg.getFirst().y().equals(seg.getSecond().y()) && seg.getFirst().y().equals(pt.y())) {
            if (seg.getFirst().x() <= pt.x() && pt.x() <= seg.getSecond().x()
                    || seg.getSecond().x() <= pt.x() && pt.x() <= seg.getFirst().x()) {
                return true;
            }
        }
        return false;
    }

    public void addCSpecCorner(Long x, Long y, List<Pair<LongPair, LongPair>> segs) {
        List<Pair<LongPair, LongPair>> toSplit = new ArrayList<>();
        for (Pair<LongPair, LongPair> seg : segs) {
            if (pointInSeg(new LongPair(x, y), seg)) {
                toSplit.add(seg);
            }
        }
        for (Pair<LongPair, LongPair> segToSplit : toSplit) {
            segs.remove(segToSplit);
            segs.add(Pair.of(new LongPair(segToSplit.getFirst().x(), segToSplit.getFirst().y()), new LongPair(x, y)));
            segs.add(Pair.of(new LongPair(x, y), new LongPair(segToSplit.getSecond().x(), segToSplit.getSecond().y())));
            if (toSplit.size() != 2) { throw new IllegalStateException("The crossing at (" + x + "," + y + ") didn't split exactly two segments"); }
        }
    }

    public List<Pair<LongPair, LongPair>> organizeEachSeg(List<Pair<LongPair, LongPair>> segs) {
        List<Pair<LongPair, LongPair>> newSegs = new ArrayList<>();
        for (Pair<LongPair, LongPair> seg : segs) {
            if (seg.getFirst().x().equals(seg.getSecond().x()) ) {
                if (seg.getFirst().y() > seg.getSecond().y()) {
                    newSegs.add(Pair.of(seg.getSecond(), seg.getFirst()));
                } else {
                    newSegs.add(Pair.of(seg.getFirst(), seg.getSecond()));
                }
            } else if (seg.getFirst().y().equals(seg.getSecond().y())) {
                if (seg.getFirst().x() > seg.getSecond().x()) {
                    newSegs.add(Pair.of(seg.getSecond(), seg.getFirst()));
                } else {
                    newSegs.add(Pair.of(seg.getFirst(), seg.getSecond()));
                }
            } else { throw new IllegalStateException("Improper segment in organizeEachSeg"); }
        }
        return newSegs;
    }

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
        return organizeEachSeg(segs);
    }

    private Pair<List<Pair<LongPair, LongPair>>, List<Pair<LongPair, LongPair>>> splitSegsIntoVH(List<Pair<LongPair, LongPair>> segs) {
        Pair<List<Pair<LongPair, LongPair>>, List<Pair<LongPair, LongPair>>> splitSegs = Pair.of(new ArrayList<>(), new ArrayList<>());
        for (Pair<LongPair, LongPair> seg : segs) {
            if (seg.getFirst().x().equals(seg.getSecond().x())) {
                splitSegs.getFirst().add(seg);
            } else if (seg.getFirst().y().equals(seg.getSecond().y())) {
                splitSegs.getSecond().add(seg);
            } else { throw new IllegalStateException("Illegal in splitSegsIntoVH"); }
        }
        return splitSegs;
    }

    private Pair<LongPair, LongPair> findLeftmostSeg(List<Pair<LongPair, LongPair>> vSegs) {
        Long min_x = vSegs.get(0).getFirst().x();
        Pair<LongPair, LongPair> leftmostSeg = vSegs.get(0);
        for (Pair<LongPair, LongPair> vSeg : vSegs) {
            Long check_x = vSeg.getFirst().x();
            if (check_x < min_x) {
                min_x = check_x;
                leftmostSeg = vSeg;
            }
        }
        return leftmostSeg;
    }

    private boolean outerContainVertical(Pair<LongPair, LongPair> vSeg1, Pair<LongPair, LongPair> vSeg2) {
        if (vSeg1.getFirst().y() <= vSeg2.getFirst().y() && vSeg1.getSecond().y() <= vSeg2.getSecond().y()) {
            return true;
        }
        if (vSeg2.getFirst().y() <= vSeg1.getFirst().y() && vSeg2.getSecond().y() <= vSeg1.getSecond().y()) {
            return true;
        }
        return false;
    }

    private boolean outerContainHorizontal(Pair<LongPair, LongPair> hSeg1, Pair<LongPair, LongPair> hSeg2) {
        if (hSeg1.getFirst().x() <= hSeg2.getFirst().x() && hSeg1.getSecond().x() <= hSeg2.getSecond().x()) {
            return true;
        }
        if (hSeg2.getFirst().x() <= hSeg1.getFirst().x() && hSeg2.getSecond().x() <= hSeg1.getSecond().x()) {
            return true;
        }
        return false;
    }

    private List<Pair<LongPair, LongPair>> getOuterBoundary(List<Pair<LongPair, LongPair>> vSegs, List<Pair<LongPair, LongPair>> hSegs) {
        List<Pair<LongPair, LongPair>> outerBoundary = new ArrayList<>();
        for (int i = 0; i < vSegs.size(); i++) {
            boolean moreLeftSegFound = false;
            boolean moreRightSegFound = false;
            Pair<LongPair, LongPair> vSeg = vSegs.get(i);
            for (int j = i + 1; j < vSegs.size(); j++) {
                Pair<LongPair, LongPair> nextVSeg = vSegs.get(j);
                if (outerContainVertical(vSeg, nextVSeg)) {
                    if (nextVSeg.getFirst().x() < vSeg.getFirst().x()) {
                        moreLeftSegFound = true;
                    }
                    if (nextVSeg.getFirst().x() > vSeg.getFirst().x()) {
                        moreRightSegFound = true;
                    }
                }
                if (moreLeftSegFound && moreRightSegFound) {
                    break;
                }
            }
            if (!moreLeftSegFound || !moreRightSegFound) {
                outerBoundary.add(vSeg);
            }
        }
        for (int i = 0; i < hSegs.size(); i++) {
            boolean moreDownSegFound = false;
            boolean moreUpSegFound = false;
            Pair<LongPair, LongPair> hSeg = hSegs.get(i);
            for (int j = i + 1; j < hSegs.size(); j++) {
                Pair<LongPair, LongPair> nextHSeg = hSegs.get(j);
                if (outerContainHorizontal(hSeg, nextHSeg)) {
                    if (nextHSeg.getFirst().y() < hSeg.getFirst().y()) {
                        moreDownSegFound = true;
                    }
                    if (nextHSeg.getFirst().x() > hSeg.getFirst().x()) {
                        moreUpSegFound = true;
                    }
                }
                if (moreDownSegFound && moreUpSegFound) {
                    break;
                }
            }
            if (!moreDownSegFound || !moreUpSegFound) {
                outerBoundary.add(hSeg);
            }
        }
        return outerBoundary;
    }

    private Pair<LongPair, LongPair> normalizeSeg(Pair<LongPair, LongPair> seg) {
        if (seg.getFirst().x() > seg.getSecond().x() || seg.getFirst().y() > seg.getSecond().y()) {
            return Pair.of(seg.getSecond(), seg.getFirst());
        }
        return seg;
    }

    private void updateSegDicts(Map<Pair<LongPair, LongPair>, Long> vSegsDict, Map<Pair<LongPair, LongPair>, Long> hSegsDict, List<Pair<LongPair, LongPair>> boundary) {
        for (Pair<LongPair, LongPair> seg : boundary) {
            if (isVertical(seg)) {
                vSegsDict.merge(normalizeSeg(seg), 1L, Long::sum);
            } else {
                hSegsDict.merge(normalizeSeg(seg), 1L, Long::sum);
            }
        }
    }

    private boolean allSegsUsed(Map<Pair<LongPair, LongPair>, Long> vSegsDict, Map<Pair<LongPair, LongPair>, Long> hSegsDict) {
        for (Long used : vSegsDict.values()) {
            if (used < 2) { return false; }
        }
        for (Long used : hSegsDict.values()) {
            if (used < 2) { throw new IllegalStateException("Horizontal segments remain with no remaining vertical segments"); }
        }
        return true;
    }

    private List<Pair<LongPair, LongPair>> remainingSegsFromDict(Map<Pair<LongPair, LongPair>, Long> segsDict) {
        List<Pair<LongPair, LongPair>> segs = new ArrayList<>();
        for (Map.Entry<Pair<LongPair, LongPair>, Long> entry : segsDict.entrySet()) {
            if (entry.getValue() < 2) {
                segs.add(entry.getKey());
            }
        }
        return segs;
    }

    private Pair<LongPair, LongPair> getFirstHSeg(List<Pair<LongPair, LongPair>> hSegs, LongPair lastPt) {
        for (Pair<LongPair, LongPair> hSeg : hSegs) {
            if (hSeg.getFirst().equals(lastPt)) {
                return hSeg;
            }
        }

        return null;
    }

    private List<Pair<LongPair, LongPair>> getNextBoundaryLineUpToX(List<Pair<LongPair, LongPair>> vSegs, List<Pair<LongPair, LongPair>> hSegs, LongPair lastPt, Long xStop, Boolean isTopOg) {
        List<Pair<LongPair, LongPair>> nextLine = new ArrayList<>();
        boolean isTop = isTopOg;
        boolean biasVBackwards = false;
        while (lastPt.x() <= xStop) {
            boolean foundSeg = false;
            if (biasVBackwards) {
                isTop = !isTop;
                biasVBackwards = false;
            }
            for (Pair<LongPair, LongPair> vSeg : vSegs) {
                if (isTop && vSeg.getSecond().equals(lastPt) || !isTop && vSeg.getFirst().equals(lastPt)) {
                    nextLine.add(vSeg);
                    if (vSeg.getSecond().equals(lastPt)) {
                        lastPt = vSeg.getFirst();
                    } else {
                        lastPt = vSeg.getSecond();
                    }
                    foundSeg = true;
                    break;
                }
            }
            if (!foundSeg) {
                for (Pair<LongPair, LongPair> vSeg : vSegs) {
                    if (vSeg.getSecond().equals(lastPt)) {
                        nextLine.add(Pair.of(vSeg.getSecond(), vSeg.getFirst()));
                        lastPt = vSeg.getFirst();
                        break;
                    }
                }
            }
            foundSeg = false;
            for (Pair<LongPair, LongPair> hSeg : hSegs) {
                if (hSeg.getFirst().equals(lastPt)) {
                    nextLine.add(hSeg);
                    lastPt = hSeg.getSecond();
                    foundSeg = true;
                    break;
                }
            }
            if (!foundSeg) {
                for (Pair<LongPair, LongPair> hSeg : hSegs) {
                    if (hSeg.getSecond().equals(lastPt)) {
                        nextLine.add(Pair.of(hSeg.getSecond(), hSeg.getFirst()));
                        lastPt = hSeg.getFirst();
                        biasVBackwards = true;
                        break;
                    }
                }
            }
            isTop = isTopOg;
        }
        return nextLine;
    }

    private List<Pair<LongPair, LongPair>> nextBoundary(List<Pair<LongPair, LongPair>> vSegs, List<Pair<LongPair, LongPair>> hSegs) {
        boolean boundaryComplete = false;
        List<Pair<LongPair, LongPair>> boundary = new ArrayList<>();
        Pair<LongPair, LongPair> leftmostSeg = findLeftmostSeg(vSegs);
        boundary.add(leftmostSeg);
        List<Pair<LongPair, LongPair>> topPart = new ArrayList<>();
        List<Pair<LongPair, LongPair>> botPart = new ArrayList<>();
        topPart.add(getFirstHSeg(hSegs, leftmostSeg.getSecond()));
        botPart.add(getFirstHSeg(hSegs, leftmostSeg.getFirst()));
        Long topXStop = 1L;
        Long botXStop = 1L;

        if (topPart.get(0).getSecond().x().equals(botPart.get(0).getSecond().x())) {
            for (Pair<LongPair, LongPair> vSeg : vSegs) {
                if (topPart.get(0).getSecond().equals(vSeg.getSecond()) && botPart.get(0).getSecond().equals(vSeg.getFirst())) {
                    boundary.addAll(topPart);
                    boundary.addAll(botPart);
                    boundary.add(vSeg);
                    return boundary;
                }
            }
        } else if (topPart.get(0).getSecond().x() < botPart.get(0).getSecond().x()) {
            topXStop = botPart.get(0).getSecond().x();
        } else {
            botXStop = topPart.get(0).getSecond().x();
        }
        LongPair topSegConnectPt = topPart.get(0).getSecond();
        LongPair botSegConnectPt = botPart.get(0).getSecond();

        while (!boundaryComplete) {
            Pair<LongPair, LongPair> topEndingHSeg = topPart.get(topPart.size() - 1);
            Pair<LongPair, LongPair> botEndingHSeg = botPart.get(botPart.size() - 1);
            if (topEndingHSeg.getSecond().x() < topXStop) {
                topPart.addAll(getNextBoundaryLineUpToX(vSegs, hSegs, topSegConnectPt, topXStop, true));
                topEndingHSeg = topPart.get(topPart.size() - 1);
            }
            if (botEndingHSeg.getSecond().x() < botXStop) {
                botPart.addAll(getNextBoundaryLineUpToX(vSegs, hSegs, botSegConnectPt, botXStop, false));
                botEndingHSeg = botPart.get(botPart.size() - 1);
            }
            if (topEndingHSeg.getSecond().x().equals(botEndingHSeg.getSecond().x())) {
                for (Pair<LongPair, LongPair> vSeg : vSegs) {

                    if (vSeg.getFirst().x().equals(topEndingHSeg.getSecond().x())) {//choice of x wlog
                        if (vSeg.getFirst().equals(botEndingHSeg.getSecond()) && vSeg.getSecond().equals(topEndingHSeg.getSecond())) {
                            boundary.add(vSeg);
                            boundaryComplete = true;
                            break;
                        }
                    }
                }
                if (!boundaryComplete) {
                    throw new IllegalStateException("This one seems to be illegal");
                }
                boundary.addAll(topPart);
                boundary.addAll(botPart);
                break;
            }
            if (topEndingHSeg.getSecond().x().equals(botEndingHSeg.getSecond().x())) {
                throw new IllegalStateException("Hit non-equal lines with the same x value");
            } else if (topEndingHSeg.getSecond().x() < botEndingHSeg.getSecond().x()) {
                topXStop = botEndingHSeg.getSecond().x();
                topSegConnectPt = topEndingHSeg.getSecond();
            } else {
                botXStop = topEndingHSeg.getSecond().x();
                botSegConnectPt = botEndingHSeg.getSecond();
            }
        }
        return boundary;
    }

    //Get the leftmost seg
    //Go off of the top and bottom once, check which gets to a further x off of the first hSeg
    //Freeze the further one and catch the other up
    //If the one that's behind catches up on the same x check for a connecting segment, otherwise
    //Continue going with the top line looking right biasing down
    //That way I don't have to deal with changing the bias for the hSegs
    //Vertical is down to up
    //Horizontal is left to right
    public List<List<Pair<LongPair, LongPair>>> getBoundaries() {
        List<List<Pair<LongPair, LongPair>>> boundaries = new ArrayList<>();
        List<Pair<LongPair, LongPair>> segs = getSegments();
        Pair<List<Pair<LongPair, LongPair>>, List<Pair<LongPair, LongPair>>> segsSplit = splitSegsIntoVH(segs);
        Map<Pair<LongPair, LongPair>, Long> vSegsDict = new HashMap<>();
        Map<Pair<LongPair, LongPair>, Long> hSegsDict = new HashMap<>();
        for (Pair<LongPair, LongPair> vSeg : segsSplit.getFirst()) {
            vSegsDict.put(vSeg, 0L);
        }
        for (Pair<LongPair, LongPair> hSeg : segsSplit.getSecond()) {
            hSegsDict.put(hSeg, 0L);
        }
        //Can rewrite outerBoundary logic to get the boundary the same way with the opposite biases I think
        List<Pair<LongPair, LongPair>> outerBoundary = getOuterBoundary(new ArrayList<>(vSegsDict.keySet()), new ArrayList<>(hSegsDict.keySet()));
        boundaries.add(outerBoundary);
        updateSegDicts(vSegsDict, hSegsDict, outerBoundary);
        boolean boundariesFound = false;
        while (!boundariesFound) {
            List<Pair<LongPair, LongPair>> vSegs = remainingSegsFromDict(vSegsDict);
            List<Pair<LongPair, LongPair>> hSegs = remainingSegsFromDict(hSegsDict);

            List<Pair<LongPair, LongPair>> nextBoundary = nextBoundary(vSegs, hSegs);

            boundaries.add(nextBoundary);
            updateSegDicts(vSegsDict, hSegsDict, nextBoundary);
            if (allSegsUsed(vSegsDict, hSegsDict)) {
                boundariesFound = true;
            }
        }
        return boundaries;
    }
}
