package com.knots.backend.repositories;


import com.knots.backend.models.entities.ExperimentStates;
import com.knots.backend.models.keys.ExperimentStatesKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExperimentStatesRepo extends JpaRepository<ExperimentStates, ExperimentStatesKey> {

    @Query("""
    SELECT e.id.ogKnotId
    FROM ExperimentStates e
    WHERE e.id.experimentId = :experimentId
      AND e.id.stateNum = :stateNum
    """)
    List<Long> findOgKnotIdsByExperimentIdAndStateNum(@Param("experimentId") Long experimentId, @Param("stateNum") Long stateNum);

}
