package com.knots.backend.services;

import com.knots.backend.repositories.InvariantRolfRepo;
import com.knots.backend.models.dtos.InvariantsDto;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvariantRolfService {

    private final InvariantRolfRepo invariantRolfRepo;

    public InvariantsDto getInvariantsByKnotId(Long knotId) {
        InvariantsDto invariants = invariantRolfRepo.findByKnotId(knotId);
        if (invariants == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Knot invariants not found");
        }

        return new InvariantsDto(
            invariants.alexander_polynomial(),
            invariants.determinant(),
            invariants.writhe()
        );
    }
}
