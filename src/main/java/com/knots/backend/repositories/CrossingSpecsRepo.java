package com.knots.backend.repositories;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.CrossingSpecs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CrossingSpecsRepo extends JpaRepository<CrossingSpecs, Long> {
    CrossingSpecs findByCrossingId(Long cid);

    @Query("""
            SELECT new com.knots.backend.models.dtos.LongPair(c.crossingX, c.crossingY)
            FROM CrossingSpecs c
            WHERE c.crossingId = :crossingId
    """)
    LongPair findCrossingCoordinatesFromCrossingId(@Param("crossingId") Long crossingId);

    @Query("SELECT MAX(c.crossingId) FROM CrossingSpecs c")
    Long getMaxCrossingId();

    @Query("SELECT MAX(c.csId) FROM CrossingSpecs c")
    Long getMaxCsId();

    @Query("SELECT c.diagramId FROM CrossingSpecs c WHERE c.crossingId = :crossingId")
    Long getCurrDiagramId(@Param("crossingId") Long crossingId);
    @Query("SELECT c.extension FROM CrossingSpecs c WHERE c.crossingId = :crossingId")
    Long getCurrExtension(@Param("crossingId") Long crossingId);

    @Query("""
        SELECT c.crossingId
        FROM CrossingSpecs c
        WHERE c.crossingId NOT IN :cids
        AND c.underLine IS NOT NULL
        AND c.overLine IS NOT NULL
    """)
    List<Long> getRemainingCrossingIds(@Param("cids") List<Long> cids);

}