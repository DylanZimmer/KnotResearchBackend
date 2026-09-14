package com.knots.backend.services;

import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.sage.AlexanderResponse;
import com.knots.backend.models.entities.FullNotation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InvariantCalculationsService {

    private final SageMathService sageMathService;

    //over-Strands need to be one strand
    //Those are the only strands - I need to define the arcs from the overs, then just change the unders
    private Map<Long, List<Long>> createAlexanderArcs(List<FullNotation> fnList) {
        //Need crossingId, (strands:) underBefore, underAfter, over for the alexanderArc
        Map<Long, List<Long>> alexanderArcs = new HashMap<>();
        Long arcNum = 0L;
        Map<LongPair, Long> arcsDict = new HashMap<>();
        for (FullNotation fn : fnList) {
            if (fn.getPlacement().equals("over")) {
                arcsDict.put(new LongPair(fn.getStrandBefore(), fn.getStrandAfter()), arcNum);
                alexanderArcs.put(fn.getCrossingId(), new ArrayList<>(Arrays.asList(0L, 0L, arcNum, 0L)));
                arcNum += 1L;
            }
        }
        for (FullNotation fn : fnList) {
            int changedCnt = 0;
            if (fn.getPlacement().equals("under")) {
                for (Map.Entry<LongPair, Long> entry : arcsDict.entrySet()) {
                    if (fn.getStrandBefore().equals(entry.getKey().x()) || fn.getStrandBefore().equals(entry.getKey().y())) {
                        alexanderArcs.get(fn.getCrossingId()).set(0, entry.getValue());
                        changedCnt += 1;
                    }
                    if (fn.getStrandAfter().equals(entry.getKey().x()) || fn.getStrandAfter().equals(entry.getKey().y())) {
                        alexanderArcs.get(fn.getCrossingId()).set(1, entry.getValue());
                        changedCnt += 1;
                    }
                    if (changedCnt == 2) {
                        if (fn.getSign() == 1L) {
                            alexanderArcs.get(fn.getCrossingId()).set(3, 1L);
                        } else if (fn.getSign() == -1L) {
                            alexanderArcs.get(fn.getCrossingId()).set(3, -1L);
                        }
                        break;
                    }
                }
            }
        }
        return alexanderArcs;
    }

    private LongPair[][] createAlexanderMatrix(List<FullNotation> fnList) {
        Map<Long, List<Long>> alexanderArcs = createAlexanderArcs(fnList);
        LongPair[][] alexanderMatrix = new LongPair[alexanderArcs.size()][alexanderArcs.size()];
        for (LongPair[] row : alexanderMatrix) {
            Arrays.fill(row, new LongPair(0L, 0L));
        }
        int rowIndex = 0;
        for (Map.Entry<Long, List<Long>> entry : alexanderArcs.entrySet()) {
            if (entry.getValue().get(3) == 1L) {
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(0).intValue(), new LongPair(-1L, 0L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(1).intValue(), new LongPair(0L, 1L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(2).intValue(), new LongPair(1L, -1L));
            } else if (entry.getValue().get(3) == -1L) {
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(0).intValue(), new LongPair(0L, -1L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(1).intValue(), new LongPair(1L, 0L));
                addCoefficient(alexanderMatrix[rowIndex], entry.getValue().get(2).intValue(), new LongPair(-1L, 1L));
            }
            rowIndex++;
        }
        return alexanderMatrix;
    }

    private void addCoefficient(LongPair[] row, int column, LongPair value) {
        LongPair current = row[column];
        row[column] = new LongPair(current.x() + value.x(), current.y() + value.y());
    }

    private Long calculateWrithe(List<FullNotation> fnList) {
        return fnList.stream().filter(fn -> "over".equals(fn.getPlacement())).mapToLong(FullNotation::getSign).sum();
    }

    public InvariantsDto getAllInvariants(List<FullNotation> fnList) {
        LongPair[][] alexanderMatrix = createAlexanderMatrix(fnList);
        AlexanderResponse alexanderResponse = sageMathService.calculateAlexanderPolynomial(alexanderMatrix);
        Long writhe = calculateWrithe(fnList);
        return (new InvariantsDto(alexanderResponse.polynomial(), alexanderResponse.determinant(), writhe));
    }
}
