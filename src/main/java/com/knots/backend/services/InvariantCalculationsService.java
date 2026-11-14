package com.knots.backend.services;

import com.knots.backend.models.dtos.*;

import com.knots.backend.models.dtos.sage.AlexanderResponse;
import com.knots.backend.models.entities.FullNotation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InvariantCalculationsService {

    private final SageMathService sageMathService;


    //****************************************************************************
    //Calculate matrices/objects to send to SageMath

    // Arcs for the Alexander matrix. An arc runs from one under-passage to the next, possibly
    // passing over several crossings on the way, so walk each component of the knot and start a
    // new arc after every under-passage. Returns crossingId -> [arc in, arc out, arc over] for the
    // under strand's incoming arc, its outgoing arc and the over arc. Returns null when some
    // component never passes under anything: that component can be lifted off, so the link is
    // split and its Alexander polynomial is 0.
    private Map<Long, List<Long>> createAlexanderArcs(List<FullNotation> fnList) {
        Map<Long, FullNotation> byStrandBefore = new HashMap<>();
        for (FullNotation fn : fnList) {
            byStrandBefore.put(fn.getStrandBefore(), fn);
        }
        Map<Long, Long> arcOfStrand = new HashMap<>();
        Set<FullNotation> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        long nextArc = 0L;
        for (FullNotation start : fnList) {
            if (visited.contains(start)) {
                continue;
            }
            List<FullNotation> component = new ArrayList<>();
            FullNotation current = start;
            while (current != null && visited.add(current)) {
                component.add(current);
                current = byStrandBefore.get(current.getStrandAfter());
            }
            int firstUnder = -1;
            for (int i = 0; i < component.size(); i++) {
                if (component.get(i).getPlacement() == Placement.under) {
                    firstUnder = i;
                    break;
                }
            }
            if (firstUnder == -1) {
                return null;
            }
            // Start just after an under-passage, so the walk begins at the start of an arc.
            Collections.rotate(component, -(firstUnder + 1));
            long arc = nextArc++;
            for (int i = 0; i < component.size(); i++) {
                FullNotation fn = component.get(i);
                arcOfStrand.put(fn.getStrandBefore(), arc);
                if (fn.getPlacement() == Placement.under && i < component.size() - 1) {
                    arc = nextArc++;
                }
            }
        }
        Map<Long, List<Long>> alexanderArcs = new HashMap<>();
        for (FullNotation fn : fnList) {
            List<Long> arcs = alexanderArcs.computeIfAbsent(fn.getCrossingId(), cid -> new ArrayList<>(Arrays.asList(0L, 0L, 0L)));
            if (fn.getPlacement() == Placement.under) {
                arcs.set(0, arcOfStrand.get(fn.getStrandBefore()));
                arcs.set(1, arcOfStrand.get(fn.getStrandAfter()));
            } else {
                arcs.set(2, arcOfStrand.get(fn.getStrandBefore()));
            }
        }
        return alexanderArcs;
    }

    // Null for a split link (see createAlexanderArcs).
    private LongPair[][] createAlexanderMatrix(List<FullNotation> fnList) {
        Map<Long, List<Long>> alexanderArcs = createAlexanderArcs(fnList);
        if (alexanderArcs == null) {
            return null;
        }
        LongPair[][] alexanderMatrix = new LongPair[alexanderArcs.size()][alexanderArcs.size()];
        for (LongPair[] row : alexanderMatrix) {
            Arrays.fill(row, new LongPair(0L, 0L));
        }
        Map<Long, Sign> crossingSigns = new HashMap<>();
        for (FullNotation fn : fnList) {
            crossingSigns.put(fn.getCrossingId(), fn.getSign());
        }
        int rowIndex = 0;
        for (Map.Entry<Long, List<Long>> entry : alexanderArcs.entrySet()) {
            if (crossingSigns.get(entry.getKey()) == Sign.pos) {
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(0).intValue(), new LongPair(-1L, 0L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(1).intValue(), new LongPair(0L, 1L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(2).intValue(), new LongPair(1L, -1L));
            } else if (crossingSigns.get(entry.getKey()) == Sign.neg) {
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(0).intValue(), new LongPair(0L, -1L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(1).intValue(), new LongPair(1L, 0L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(2).intValue(), new LongPair(-1L, 1L));
            }
            rowIndex++;
        }
        return alexanderMatrix;
    }

    //************************************************************************************
    //Invariants calculable from just the full notation

    private void addCoefficient(LongPair[] row, int column, LongPair value) {
        LongPair current = row[column];
        row[column] = new LongPair(current.x() + value.x(), current.y() + value.y());
    }

    private Long calculateWrithe(List<FullNotation> fnList) {
        return fnList.stream().filter(fn -> fn.getPlacement() == Placement.over).mapToLong(fn -> fn.getSign().value()).sum();
    }

    public InvariantsDto getAllInvariants(List<FullNotation> fnList) {
        Long writhe = calculateWrithe(fnList);
        if (fnList.isEmpty()) {
            // The unknot drawn with no crossings.
            return new InvariantsDto("1", 1L, writhe);
        }
        LongPair[][] alexanderMatrix = createAlexanderMatrix(fnList);
        if (alexanderMatrix == null) {
            return new InvariantsDto("0", 0L, writhe);
        }
        AlexanderResponse alexanderResponse = sageMathService.calculateAlexanderPolynomial(alexanderMatrix);
        return new InvariantsDto(Map.of(
                InvariantType.Alexander_Polynomial, alexanderResponse.polynomial(),
                InvariantType.Determinant, alexanderResponse.determinant(),
                InvariantType.Writhe, writhe
        ));
    }
}
