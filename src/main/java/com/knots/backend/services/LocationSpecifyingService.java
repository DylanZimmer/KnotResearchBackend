package com.knots.backend.services;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.MoveRequestArgs;
import com.knots.backend.models.dtos.experiments.AddTwistRequest;
import com.knots.backend.models.dtos.experiments.CrossingTypeDto;
import com.knots.backend.models.dtos.experiments.MoveRequest;
import com.knots.backend.models.dtos.experiments.StrandTypeDto;
import com.knots.backend.models.entities.FullNotation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//StrandType
    //CrossingType, CrossingType, Boolean
//CrossingType
    //Long, Position

@RequiredArgsConstructor
@Service
public class LocationSpecifyingService {

    private final FullNotationMovesService fullNotationMovesService;

    private List<Long> intersectPotentialStrandLists(List<List<Long>> potentialStrandLists) {
        if (potentialStrandLists.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> intersection = new ArrayList<>(potentialStrandLists.get(0));
        for (int i = 1; i < potentialStrandLists.size(); i++) {
            intersection.retainAll(potentialStrandLists.get(i));
        }
        return intersection;
    }

    //Does orientedWith even make sense when not talking about boundaries?
    public List<Long> identifyAllStrands(List<FullNotation> fnList, StrandTypeDto strandType, HashMap<Long, Boolean> orientedStrands) {
        List<List<Long>> potentialStrandLists = new ArrayList<>();
        if (strandType.orientedWith() != null) {
            List<Long> potentialStrands = new ArrayList<>();
            for (Map.Entry<Long, Boolean> strand : orientedStrands.entrySet()) {
                if (strand.getValue().equals(strandType.orientedWith())) {
                    potentialStrands.add(strand.getKey());
                }
            }
            potentialStrandLists.add(potentialStrands);
        }

        if (strandType.crossing_before() != null) {
            CrossingTypeDto cb = strandType.crossing_before();
            if (cb.sign() != null) {
                List<Long> potentialStrands = new ArrayList<>();
                for (FullNotation fn : fnList) {
                    if (cb.sign().equals(fn.getSign())) {
                        if (orientedStrands.get(fn.getStrandBefore())) {
                            potentialStrands.add(fn.getStrandBefore());
                        }
                        if (!orientedStrands.get(fn.getStrandAfter())) {
                            potentialStrands.add(fn.getStrandAfter());
                        }
                    }
                }
                potentialStrandLists.add(potentialStrands);
            }
            if (cb.placement() != null) {
                List<Long> potentialStrands = new ArrayList<>();
                for (FullNotation fn : fnList) {
                    if (cb.placement().equals(fn.getPlacement())) {
                        if (orientedStrands.get(fn.getStrandBefore())) {
                            potentialStrands.add(fn.getStrandBefore());
                        }
                        if (!orientedStrands.get(fn.getStrandAfter())) {
                            potentialStrands.add(fn.getStrandAfter());
                        }
                    }
                }
                potentialStrandLists.add(potentialStrands);
            }
        }
        if (strandType.crossing_after() != null) {
            CrossingTypeDto ca = strandType.crossing_after();
            if (ca.sign() != null) {
                List<Long> potentialStrands = new ArrayList<>();
                for (FullNotation fn : fnList) {
                    if (ca.sign().equals(fn.getSign())) {
                        if (!orientedStrands.get(fn.getStrandBefore())) {
                            potentialStrands.add(fn.getStrandBefore());
                        }
                        if (orientedStrands.get(fn.getStrandAfter())) {
                            potentialStrands.add(fn.getStrandAfter());
                        }
                    }
                }
                potentialStrandLists.add(potentialStrands);
            }
            if (ca.placement() != null) {
                List<Long> potentialStrands = new ArrayList<>();
                for (FullNotation fn : fnList) {
                    if (ca.placement().equals(fn.getPlacement())) {
                        if (!orientedStrands.get(fn.getStrandBefore())) {
                            potentialStrands.add(fn.getStrandBefore());
                        }
                        if (orientedStrands.get(fn.getStrandAfter())) {
                            potentialStrands.add(fn.getStrandAfter());
                        }
                    }
                }
                potentialStrandLists.add(potentialStrands);
            }
        }
        return intersectPotentialStrandLists(potentialStrandLists);
    }

    //Might be worth writing a way of doing this that goes through strands and returns the first one that hits all of the non-null specifications,
        //but doing so was yielding something not much computationally shorter, so to be revisited
    public Long identifyOneStrand(List<FullNotation> fnList, StrandTypeDto strandType, HashMap<Long, Boolean> orientedStrands) {
        return identifyAllStrands(fnList, strandType, orientedStrands).get(0);
    }

    public MoveRequestArgs convertRawRequestArgs(List<FullNotation> fnList, MoveRequest<?, ?> req, HashMap<Long, Boolean> orientedStrands) {
        MoveRequestArgs args = new MoveRequestArgs();
        if (req instanceof AddTwistRequest twistReq) {
            //return what's needed in ExperimentService
            if (!twistReq.until().equals(0L)) {
                Long strand = identifyOneStrand(fnList, twistReq.location(), orientedStrands);
                args.setStrand(strand);
                args.setSign(twistReq.typeSpecificChoice());
                return args;
            }
        }
        return args;
    }

}
