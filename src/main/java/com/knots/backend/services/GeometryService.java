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



    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<><
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






    /*
    private Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextHSegsFromLeftmostVSeg(List<Pair<LongPair, LongPair>> hSegs, Pair<LongPair, LongPair> leftmostSeg) {
        Long vSeg_x;
        if (leftmostSeg.getFirst().x().equals(leftmostSeg.getSecond().x())) {
            vSeg_x = leftmostSeg.getFirst().x();
        } else { throw new IllegalStateException("The leftmost segment isn't vertical"); }
        Pair<LongPair, LongPair> topSeg = null;
        Pair<LongPair, LongPair> botSeg = null;
        //Only checking the first coord in hSeg because the second will always be to the right of it
        for (Pair<LongPair, LongPair> hSeg : hSegs) {
            if (hSeg.getFirst().x().equals(vSeg_x)) {
                if (hSeg.getFirst().y().equals(leftmostSeg.getFirst().y())) {
                    if (topSeg == null) {
                        topSeg = hSeg;
                    } else { throw new IllegalStateException("Two segments matched the top"); }
                } else if (hSeg.getFirst().y().equals(leftmostSeg.getSecond().y())) {
                    if (botSeg == null) {
                        botSeg = hSeg;
                    } else { throw new IllegalStateException("Two segments matched the bottom."); }
                }
            }
        }
        System.out.println("Connected segs from the leftmost vertical segment");
        System.out.println(leftmostSeg);
        System.out.println(topSeg);
        System.out.println(botSeg);
        if (topSeg != null && botSeg != null) {
            return (Pair.of(topSeg, botSeg));
        } else { throw new IllegalStateException("Didn't match the top and bottom while getting the first horizontal segments from the leftomst vertical one."); }
    }

    private Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextVSegsFromLastHSegs(Pair<LongPair, LongPair> hSeg1, Pair<LongPair, LongPair> hSeg2, List<Pair<LongPair, LongPair>> vSegs) {
        Long seg1_y;
        Long seg2_y;
        Pair<LongPair, LongPair> topConnectedSeg = null;
        Pair<LongPair, LongPair> bottomConnectedSeg = null;
        if (hSeg1.getFirst().y().equals(hSeg1.getSecond().y())) {
            seg1_y = hSeg1.getFirst().y();
        } else { throw new IllegalStateException("hSeg1 isn't horizontal"); }
        if (hSeg2.getFirst().y().equals(hSeg2.getSecond().y())) {
            seg2_y = hSeg2.getFirst().y();
        } else { throw new IllegalStateException("hSeg2 isn't horizontal"); }
        LongPair bottomConnectingPoint;
        LongPair topConnectingPoint;
        if (seg1_y < seg2_y) {
            bottomConnectingPoint = hSeg1.getSecond();
            topConnectingPoint = hSeg2.getSecond();
        } else {
            topConnectingPoint = hSeg1.getSecond();
            bottomConnectingPoint = hSeg2.getSecond();
        }
        for (Pair<LongPair, LongPair> vSeg : vSegs) {
            if (topConnectingPoint.equals(vSeg.getSecond())) {
                topConnectedSeg = vSeg;
            }
            if (bottomConnectingPoint.equals(vSeg.getFirst())) {
                bottomConnectedSeg = vSeg;
            }
        }
        if (topConnectedSeg == null || bottomConnectedSeg == null) {
            for (Pair<LongPair, LongPair> vSeg : vSegs) {
                if (topConnectedSeg == null) {
                    if (topConnectingPoint.equals(vSeg.getFirst())) {
                        topConnectedSeg = vSeg;
                    }
                }
                if (bottomConnectedSeg == null) {
                    if (bottomConnectingPoint.equals(vSeg.getSecond())) {
                        bottomConnectedSeg = vSeg;
                    }
                }
                if (topConnectedSeg != null && bottomConnectedSeg != null) {
                    break;
                }
            }
        }
        System.out.println("Connected segs in nextVSegsFromLastHSegs");
        System.out.println(hSeg1);
        System.out.println(hSeg2);
        System.out.println(topConnectedSeg);
        System.out.println(bottomConnectedSeg);
        if (topConnectedSeg != null && bottomConnectedSeg != null) {
            return (Pair.of(bottomConnectedSeg, topConnectedSeg));
        } else { throw new IllegalStateException("Didn't match the top and bottom of the previous horizontals next vertical segments from the last horizontals."); }
    }


    private Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextHSegsFromLastVSegs(Pair<LongPair, LongPair> vSeg1, Pair<LongPair, LongPair> vSeg2, List<Pair<LongPair, LongPair>> hSegs) {
        Long seg1_x;
        Long seg2_x;
        Pair<LongPair, LongPair> leftConnectedSeg = null;
        Pair<LongPair, LongPair> rightConnectedSeg = null;
        if (vSeg1.getFirst().x().equals(vSeg1.getSecond().x())) {
            seg1_x = vSeg1.getFirst().x();
        } else { throw new IllegalStateException("vSeg1 isn't vertical"); }
        if (vSeg2.getFirst().x().equals(vSeg2.getSecond().x())) {
            seg2_x = vSeg2.getFirst().x();
        } else { throw new IllegalStateException("vSeg2 isn't vertical"); }
        LongPair leftConnectingPoint;
        LongPair rightConnectingPoint;
        if (seg1_x < seg2_x) {
            leftConnectingPoint = vSeg1.getSecond();
            rightConnectingPoint = vSeg2.getSecond();
        } else {
            rightConnectingPoint = vSeg1.getSecond();
            leftConnectingPoint = vSeg2.getSecond();
        }
        for (Pair<LongPair, LongPair> hSeg : hSegs) {
            if (rightConnectingPoint.equals(hSeg.getSecond())) {
                rightConnectedSeg = hSeg;
            }
            if (leftConnectingPoint.equals(hSeg.getFirst())) {
                leftConnectedSeg = hSeg;
            }
        }
        if (rightConnectedSeg == null || leftConnectedSeg == null) {
            for (Pair<LongPair, LongPair> hSeg : hSegs) {
                if (rightConnectedSeg == null) {
                    if (rightConnectingPoint.equals(hSeg.getFirst())) {
                        rightConnectedSeg = hSeg;
                    }
                }
                if (leftConnectedSeg == null) {
                    if (leftConnectingPoint.equals(hSeg.getSecond())) {
                        leftConnectedSeg = hSeg;
                    }
                }
                if (rightConnectedSeg != null && leftConnectedSeg != null) {
                    break;
                }
            }
        }
        System.out.println("Connected segs in nextHSegsFromLastVSegs");
        System.out.println(vSeg1);
        System.out.println(vSeg2);
        System.out.println(leftConnectedSeg);
        System.out.println(rightConnectedSeg);
        if (rightConnectedSeg != null && leftConnectedSeg != null) {
            return (Pair.of(leftConnectedSeg, rightConnectedSeg));
        } else { throw new IllegalStateException("Didn't match the left and right while getting the first vertical segments from the last horizontals."); }
    }

    //Start by getting the leftmost segment, then the boundary should start at the top point and stop at the bottom one
    //calls will just be getHSegFromVSeg and vice versa and will include a flag for top/bot l/r
    //A top line should look for a line connecting down, a right should look
    //But what is a top line???
    //A top line is a horizontal line that connects to the top point of the vertical line to the left of it
    //Horizontal lines should bias toward going the same way as the line directly before it

    private Pair<LongPair, LongPair> nextHSegHelper(LongPair pt, List<Pair<LongPair, LongPair>> hSegs, Boolean probablyGoingRight) {
        if (probablyGoingRight) {
            for (Pair<LongPair, LongPair> hSeg : hSegs) {
                if (hSeg.getFirst().equals(pt)) {
                    return hSeg;
                }
            }
        } else {
            for (Pair<LongPair, LongPair> hSeg : hSegs) {
                if (hSeg.getSecond().equals(pt)) {
                    return hSeg;
                }
            }
        }
        return null;
    }

    private Pair<Pair<LongPair, LongPair>, Boolean> getNextHSeg(LongPair pt, List<Pair<LongPair, LongPair>> hSegs, Boolean probablyGoingRight) {
        boolean changeExpectedDirection = false;
        Pair<LongPair, LongPair> newHSeg;
        newHSeg = nextHSegHelper(pt, hSegs, probablyGoingRight);
        if (newHSeg == null) {
            newHSeg = nextHSegHelper(pt, hSegs, !probablyGoingRight);
            if (newHSeg != null) {
                changeExpectedDirection = true;
            }
        }
        return Pair.of(newHSeg, changeExpectedDirection);
    }

    private Pair<LongPair, LongPair> nextVSegHelper(LongPair pt, List<Pair<LongPair, LongPair>> vSegs, Boolean topSeg) {
        if (topSeg) {
            for (Pair<LongPair, LongPair> vSeg : vSegs) {
                if (vSeg.getSecond().equals(pt)) {
                    return vSeg;
                }
            }
        } else {
            for (Pair<LongPair, LongPair> vSeg : vSegs) {
                if (vSeg.getFirst().equals(pt)) {
                    return vSeg;
                }
            }
        }
        return null;
    }

    private Pair<LongPair, LongPair> getNextVSeg(LongPair pt, List<Pair<LongPair, LongPair>> vSegs, Boolean topSeg) {
        Pair<LongPair, LongPair> newVSeg;
        newVSeg = nextHSegHelper(pt, vSegs, topSeg);
        if (newVSeg == null) {
            newVSeg = nextHSegHelper(pt, vSegs, !topSeg);
        }
        return newVSeg;
    }
    */


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

    private void updateSegDicts(Map<Pair<LongPair, LongPair>, Long> vSegsDict, Map<Pair<LongPair, LongPair>, Long> hSegsDict, List<Pair<LongPair, LongPair>> boundary) {
        for (Pair<LongPair, LongPair> seg : boundary) {
            if (isVertical(seg)) {
                vSegsDict.merge(seg, 1L, Long::sum);
            } else {
                hSegsDict.merge(seg, 1L, Long::sum);
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
        while (lastPt.x() < xStop) {
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
        Long topXStop = 0L;
        Long botXStop = 0L;
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
        //Need to get topXStop and botXStop - should I do one outside, or move it to the top of the while loop?
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
                    System.out.println("Hit the equals that should end the boundary");
                    System.out.println("vSeg :  " + vSeg);
                    System.out.println("topEndingHSeg : " + topEndingHSeg);
                    System.out.println("botEndingHSeg : " + botEndingHSeg);
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








    /*
    //Vertical is down to up
    //Horizontal is left to right
    private List<Pair<LongPair, LongPair>> nextBoundaryLessOld(List<Pair<LongPair, LongPair>> vSegsInit, List<Pair<LongPair, LongPair>> hSegsInit) {
        List<Pair<LongPair, LongPair>> vSegs = vSegsInit;
        List<Pair<LongPair, LongPair>> hSegs = hSegsInit;
        List<Pair<LongPair, LongPair>> nextBoundary = new ArrayList<>();
        Pair<LongPair, LongPair> leftmostSeg = findLeftmostSeg(vSegs);
        Pair<LongPair, LongPair> nextVSeg = leftmostSeg;
        boolean probablyGoingRight = true;
        boolean connectedOnTop = false;
        boolean boundaryComplete = false;
        while (!boundaryComplete) {
            //Need to give it the first entry of nextVSeg sometimes
            //
            Pair<Pair<LongPair, LongPair>, Boolean> nextHSegWSwitchBool = getNextHSeg(nextVSeg.getSecond(), hSegs, probablyGoingRight);
            Pair<LongPair, LongPair> nextHSeg = nextHSegWSwitchBool.getFirst();
            if (nextHSegWSwitchBool.getSecond()) {
                probablyGoingRight = !probablyGoingRight;
            }
            if (probablyGoingRight) {   //Right check?
                getNextVSeg(nextHSeg.getSecond(), hSegs);
            } else {
                getNextVSeg(nextHSeg.getFirst(), hSegs);
            }

        return nextBoundary;
    }


    private List<Pair<LongPair, LongPair>> nextBoundaryOld(List<Pair<LongPair, LongPair>> vSegsInit, List<Pair<LongPair, LongPair>> hSegsInit) {
        List<Pair<LongPair, LongPair>> vSegs = vSegsInit;
        List<Pair<LongPair, LongPair>> hSegs = hSegsInit;
        List<Pair<LongPair, LongPair>> nextBoundary = new ArrayList<>();
        Pair<LongPair, LongPair> leftmostSeg = findLeftmostSeg(vSegs);
        nextBoundary.add(leftmostSeg);
        Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextHSegs = nextHSegsFromLeftmostVSeg(hSegs, leftmostSeg);
        nextBoundary.add(nextHSegs.getFirst());
        nextBoundary.add(nextHSegs.getSecond());
        boolean boundaryComplete = false;
        while (!boundaryComplete) {
            Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextVSegs = nextVSegsFromLastHSegs(nextHSegs.getFirst(), nextHSegs.getSecond(), vSegs);
            if (nextVSegs.getFirst().equals(nextVSegs.getSecond())) {
                nextBoundary.add(nextVSegs.getFirst());
                boundaryComplete = true;
                break;
            }
            nextBoundary.add(nextVSegs.getFirst());
            nextBoundary.add(nextVSegs.getSecond());

            nextHSegs = nextHSegsFromLastVSegs(nextVSegs.getFirst(), nextVSegs.getSecond(), hSegs);
            nextBoundary.add(nextHSegs.getFirst());
            nextBoundary.add(nextHSegs.getSecond());
            System.out.println("nextVSegs   : " + nextVSegs);
            System.out.println("nextHSegs   : " + nextHSegs);
            System.out.println("nextBoundary   : " + nextBoundary);
            vSegs.remove(nextVSegs.getFirst());
            vSegs.remove(nextVSegs.getSecond());
            hSegs.remove(nextHSegs.getFirst());
            hSegs.remove(nextHSegs.getSecond());
        }
        return nextBoundary;
    }


    //I can go through the vertical segs and make sure each appears twice. Then I can assert check that each horizontal does too
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
        List<Pair<LongPair, LongPair>> outerBoundary = getOuterBoundary(new ArrayList<>(vSegsDict.keySet()), new ArrayList<>(hSegsDict.keySet()));
        boundaries.add(outerBoundary);
        updateSegDicts(vSegsDict, hSegsDict, outerBoundary);
        boolean boundariesFound = false;
        while (!boundariesFound) {
            List<Pair<LongPair, LongPair>> vSegs = remainingSegsFromDict(vSegsDict);
            System.out.println("Hit vSegs :   " + vSegs);
            List<Pair<LongPair, LongPair>> hSegs = remainingSegsFromDict(hSegsDict);
            System.out.println("Hit hSegs :   " + hSegs);
            List<Pair<LongPair, LongPair>> nextBoundary = nextBoundary(vSegs, hSegs);
            System.out.println("Next Boundary");
            System.out.println(nextBoundary);
            System.out.println("Next vSegsDict");
            System.out.println(vSegsDict);
            System.out.println("Next hSegsDict");
            System.out.println(hSegsDict);
            if (boundaries.contains(nextBoundary)) {
                System.out.println("DOUBLE CONTAINS");
                boundariesFound = true;
                break;
            }
            boundaries.add(nextBoundary);
            updateSegDicts(vSegsDict, hSegsDict, nextBoundary);
            boundariesFound = allSegsUsed(vSegsDict, hSegsDict);
        }
        return boundaries;
    }
    */



    //*************************************************************************************************
    //Below is back to walks - I want a full list of
    /*
    - Get a partition of walks. Go through, starting at C0, splitting in all 4 directions.
    Keep a list with all walks. Whenever a loop is closed, put that in its own list. Do that until all loops
    are closed.
    - This is list of borders that contain one or more boundary. One of them contains the full inside and the
    outside. But instead of determining which it will probably be easier to just calculate the outline as
    done below for the space outside the knot
    - Clean up the list of borders. Check for where borders get cut off. I need to deal with the problem of
    the outer border. If I'm asking it to find all inner boundaries from it recursively that's the entire
    problem and there's no need for anything else. If I'm only finding the first border within it then what's the
    analagous to some boundary that splits two borders?




    It might just straight up be a better idea to do the recursive outline thing. First I'm going to


    Maybe recursive from the smallest boundaries out. Think about the next boundary out in terms of connections,
    do the horizontal and vertical line belong to the same corner? What's the analagous for a crossing? And then
    for chains of such. Build the boundaries out until I find the outer border
    */



    //************************************************************************************************
    //Below is the method where I find the outer border then work inwards
    /*
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
            if (toSplit.size() != 2) {
                System.out.println("The crossing at (" + x + "," + y + ") didn't split exactly two segments");
            }
        }
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

    public boolean isVertical(Pair<LongPair, LongPair> seg) {
        if (seg.getFirst().x().equals(seg.getSecond().x())) {
            return true;
        } else {
            return false;
        }
    }

    public List<Pair<LongPair, LongPair>> organizeBoundary(List<Pair<LongPair, LongPair>> boundary) {
        List<Pair<LongPair, LongPair>> fullBoundary = new ArrayList<>(boundary);
        List<Pair<LongPair, LongPair>> newBoundary = new ArrayList<>();
        newBoundary.add(fullBoundary.get(0));
        fullBoundary.remove(0);
        while (!fullBoundary.isEmpty()) {
            Pair<LongPair, LongPair> latestSeg = newBoundary.get(newBoundary.size() - 1);
            boolean found = false;
            Iterator<Pair<LongPair, LongPair>> it = fullBoundary.iterator();
            while (it.hasNext()) {
                Pair<LongPair, LongPair> seg = it.next();
                if (latestSeg.getSecond().x().equals(seg.getFirst().x()) && latestSeg.getSecond().y().equals(seg.getFirst().y())) {
                    newBoundary.add(seg);
                    it.remove();
                    found = true;
                    break;
                }
            }
            if (!found) {
                //throw new IllegalStateException("Boundary is not a continuous loop.");
                return null;
            }
        }
        return newBoundary;
    }

    private boolean segAContainedInB(Pair<LongPair,LongPair> segA, Pair<LongPair, LongPair> segB, Boolean BInAAllowed) {
        boolean v;
        if (isVertical(segA) == isVertical(segB)) {
            if (isVertical(segA)) {
                v = true;
            } else {
                v = false;
            }
        } else { throw new IllegalStateException("Segments don't match."); }
        if (v) {
            if (segB.getFirst().y() <= segA.getFirst().y() && segA.getSecond().y() <= segB.getSecond().y()) {
                return true;
            }
            return BInAAllowed && segA.getFirst().y() <= segB.getFirst().y() && segB.getSecond().y() <= segA.getSecond().y();
        } else {
            if (segB.getFirst().x() <= segA.getFirst().x() && segA.getSecond().x() <= segB.getSecond().x()) {
                return true;
            }
            return BInAAllowed && segA.getFirst().x() <= segB.getFirst().x() && segB.getSecond().x() <= segA.getSecond().x();
        }
    }

    private Direction segInOutline(Pair<LongPair, LongPair> seg, List<Pair<LongPair, LongPair>> segs) {
        boolean u_r_line = false;
        boolean d_l_line = false;
        boolean v = isVertical(seg);
        if (v) {
            for (Pair<LongPair, LongPair> checkSeg : segs) {
                if (isVertical(checkSeg)) {
                    if (checkSeg.getFirst().x() < seg.getFirst().x() && segAContainedInB(seg, checkSeg, true)) {
                        d_l_line = true;
                    }
                    if (seg.getFirst().x() < checkSeg.getFirst().x() && segAContainedInB(seg, checkSeg, true)) {
                        u_r_line = true;
                    }
                }

            }
        } else {
            for (Pair<LongPair, LongPair> checkSeg : segs) {
                if (!isVertical(checkSeg)) {
                    if (checkSeg.getFirst().y() < seg.getFirst().y() && segAContainedInB(seg, checkSeg, true)) {
                        d_l_line = true;
                    }
                    if (seg.getFirst().y() < checkSeg.getFirst().y() && segAContainedInB(seg, checkSeg, true)) {
                        u_r_line = true;
                    }
                }
            }
        }
        if (!u_r_line || !d_l_line) {
            if (v) {
                if (!u_r_line) {
                    return Direction.R;
                } else {
                    return Direction.L;
                }
            } else {
                if (!u_r_line) {
                    return Direction.U;
                } else {
                    return Direction.D;
                }
            }
        } else {
            return null;
        }
    }

    public Map<Pair<LongPair, LongPair>, Direction> getOutlineFromSegs(List<Pair<LongPair, LongPair>> segs) {
        Map<Pair<LongPair, LongPair>, Direction> outline = new HashMap<>();
        for (Pair<LongPair, LongPair> seg : segs) {
            Direction noFurtherSegsDn = segInOutline(seg, segs);
            if (noFurtherSegsDn != null) {
                outline.put(seg, noFurtherSegsDn);
            }
        }
        //return organizeBoundary(new ArrayList<>(outline.keySet()));
        return outline;
    }

    private Boolean connectedSegPairs(Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextSegPair, Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> segPair) {
        if (nextSegPair.getFirst().getFirst().equals(segPair.getFirst().getFirst())
        || nextSegPair.getFirst().getSecond().equals(segPair.getFirst().getSecond())
        || nextSegPair.getSecond().getFirst().equals(segPair.getSecond().getFirst())
        || nextSegPair.getSecond().getSecond().equals(segPair.getSecond().getSecond())) {
            return true;
        }
        return false;
    }

    private List<List<Pair<LongPair, LongPair>>> innerBoundariesFromOutline(Map<Pair<LongPair, LongPair>, Direction> outline, List<Pair<LongPair, LongPair>> remainingSegs) {
        //First get pairs of segments, then pair them together
        //After all pairs are together, I need to fit them together into boundaries
        List<List<Pair<LongPair, LongPair>>> newBoundaries = new ArrayList<>();
        List<Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>>> segPairs = new ArrayList<>();
        for (Map.Entry<Pair<LongPair, LongPair>, Direction> entry : outline.entrySet()) {
            int minDist = Integer.MAX_VALUE;
            Pair<Pair<LongPair, LongPair>, Boolean> pairSeg = null; //boolean for outlineSegFirst
            Pair<LongPair, LongPair> outlineSeg = entry.getKey();
            for (Pair<LongPair, LongPair> seg : remainingSegs) {
                if (isVertical(seg) && isVertical(outlineSeg)) {
                    if (entry.getValue().equals(Direction.R)) {
                        if (seg.getFirst().x() < outlineSeg.getFirst().x() && (segAContainedInB(seg, outlineSeg, true))) {
                            int dist = (int) (outlineSeg.getFirst().x() - seg.getFirst().x());
                            if (dist < minDist) {
                                minDist = dist;
                                pairSeg = Pair.of(seg, false);
                            }
                        }
                    } else if (entry.getValue().equals(Direction.L)) {
                        if (seg.getFirst().x() > outlineSeg.getFirst().x() && segAContainedInB(seg, outlineSeg, true)) {
                            int dist = (int) (seg.getFirst().x() - outlineSeg.getFirst().x());
                            if (dist < minDist) {
                                minDist = dist;
                                pairSeg = Pair.of(seg, true);
                            }
                        }
                    }
                } else if (!isVertical(seg) && !isVertical(entry.getKey())) {
                    if (entry.getValue().equals(Direction.U)) {
                        if (seg.getFirst().y() < outlineSeg.getFirst().y() && segAContainedInB(seg, outlineSeg, true)) {
                            int dist = (int) (outlineSeg.getFirst().y() - seg.getFirst().y());
                            if (dist < minDist) {
                                minDist = dist;
                                pairSeg = Pair.of(seg, false);
                            }
                        }
                    } else if (entry.getValue().equals(Direction.D)) {
                        if (seg.getFirst().y() > outlineSeg.getFirst().y() && segAContainedInB(seg, outlineSeg, true)) {
                            int dist = (int) (seg.getFirst().y() - outlineSeg.getFirst().y());
                            if (dist < minDist) {
                                minDist = dist;
                                pairSeg = Pair.of(seg, true);
                            }
                        }
                    }
                }
            }
            if (pairSeg == null) {
                throw new IllegalStateException("No closest across segment");
            } else {
                if (pairSeg.getSecond()) {
                    segPairs.add(Pair.of(outlineSeg, pairSeg.getFirst()));
                } else {
                    segPairs.add(Pair.of(pairSeg.getFirst(), outlineSeg));
                }
            }
        }
        while (!segPairs.isEmpty()) {
            Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> nextSegPair = segPairs.get(0);
            List<Pair<LongPair, LongPair>> newBoundary = new ArrayList<>();
            newBoundary.add(nextSegPair.getFirst());
            newBoundary.add(nextSegPair.getSecond());
            Boolean isFullBoundary = false;
            while (!isFullBoundary) {
                if (isVertical(nextSegPair.getFirst())) {
                    for (Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> segPair : segPairs) {
                        if (!isVertical(segPair.getFirst())) {
                            Boolean isConnected = connectedSegPairs(nextSegPair, segPair);
                            if (isConnected) {
                                newBoundary.add(segPair.getFirst());
                                newBoundary.add(segPair.getSecond());
                                List<Pair<LongPair, LongPair>> potentialNewBoundary = organizeBoundary(newBoundary);
                                if (potentialNewBoundary != null) {
                                    newBoundary = potentialNewBoundary;
                                    isFullBoundary = true;
                                    System.out.println("(((((((((((((****************&&&&&&&&&&&Completed Boundary : ");
                                    System.out.println(newBoundary);
                                    break;
                                }
                                nextSegPair = segPair;
                            }
                        }
                    }
                } else {
                    for (Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> segPair : segPairs) {
                        if (isVertical(segPair.getFirst())) {
                            Boolean isConnected = connectedSegPairs(nextSegPair, segPair);
                            if (isConnected) {
                                newBoundary.add(segPair.getFirst());
                                newBoundary.add(segPair.getSecond());
                                List<Pair<LongPair, LongPair>> potentialNewBoundary = organizeBoundary(newBoundary);
                                if (potentialNewBoundary != null) {
                                    newBoundary = potentialNewBoundary;
                                    isFullBoundary = true;
                                    break;
                                }
                                nextSegPair = segPair;
                            }
                        }
                    }
                }
            }
            for (Pair<LongPair, LongPair> seg : newBoundary) {
                Iterator<Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>>> it = segPairs.iterator();
                while (it.hasNext()) {
                    Pair<Pair<LongPair, LongPair>, Pair<LongPair, LongPair>> segPair = it.next();
                    if (segPair.getFirst().equals(seg) || segPair.getSecond().equals(seg)) {
                        it.remove();
                    }
                }
            }
            newBoundaries.add(newBoundary);
        }
        return newBoundaries;
    }

    //public List<List<Pair<LongPair, LongPair>>> getBoundaries() {
    public List<List<Pair<LongPair, LongPair>>> getBoundaries() {
        List<List<Pair<LongPair, LongPair>>> boundaries = new ArrayList<>();
        List<Pair<LongPair, LongPair>> remainingSegs = getSegments();
        boolean outerBoundary = true;
        while (!remainingSegs.isEmpty()) {
            Map<Pair<LongPair, LongPair>, Direction> outline = getOutlineFromSegs(remainingSegs);
            if (outerBoundary) {
                boundaries.add(organizeBoundary(new ArrayList<>(outline.keySet())));
                outerBoundary = false;
            }
            boundaries.addAll(innerBoundariesFromOutline(outline, remainingSegs));
            for (Pair<LongPair, LongPair> seg : outline.keySet()) {
                remainingSegs.remove(seg);
            }
            System.out.println("remainingSegs in loop : ");
            System.out.println(remainingSegs);
        }
        return boundaries;
    };
    */
    //Above is boundaries using the outside borders method
    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    /* Below is boundaries from the sweep method
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
    */





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
