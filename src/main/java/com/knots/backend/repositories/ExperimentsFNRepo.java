package com.knots.backend.repositories;

import com.knots.backend.models.entities.ExperimentsFullNotation;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.models.keys.ExperimentsFNKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExperimentsFNRepo extends JpaRepository<ExperimentsFullNotation, ExperimentsFNKey> {
    @Query("""
        SELECT new com.knots.backend.models.entities.FullNotation(
            e.id.ogKnotId, e.id.crossingId, e.id.placement,
            e.cidBefore, e.cidAfter, e.strandBefore, e.strandAfter, e.sign)
        FROM ExperimentsFullNotation e
        WHERE e.id.experimentId = :experimentId
        AND e.id.ogKnotId = :ogKnotId
        AND e.id.stateNum = :stateNum
    """)
    List<FullNotation> findFnListFromState(@Param("experimentId") Long experimentId, @Param("ogKnotId") Long ogKnotId, @Param("stateNum") Long stateNum);
}