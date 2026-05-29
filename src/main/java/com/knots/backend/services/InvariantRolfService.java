package com.knots.backend.services;

import com.knots.backend.models.entities.InvariantsRolf;
import com.knots.backend.repositories.InvariantRolfRepo;
import com.knots.backend.models.dtos.InvariantDto;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvariantRolfService {

    private final InvariantRolfRepo invariantRolfRepo;

    public InvariantDto getInvariantsByKnotId(Long knotId) {
        InvariantsRolf invariants = invariantRolfRepo.findByKnotId(knotId);

        return new InvariantDto(
                invariants.getDeterminant(),
                invariants.getAlexander_polynomial(),
                invariants.getJones_polynomial()
        );
    }
}
