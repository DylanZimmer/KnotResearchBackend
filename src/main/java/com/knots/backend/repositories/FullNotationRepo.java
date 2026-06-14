package com.knots.backend.repositories;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FullNotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FullNotationRepo extends JpaRepository<FullNotation, Long> {
    @Query("""
        SELECT new com.knots.backend.models.dtos.LongPair(fn.arcIn, fn.arcOut)
        FROM FullNotation fn
        WHERE fn.crossingId = :cid
        AND fn.placement = :placement
    """)
    LongPair getArcs(@Param("cid") Long cid, @Param("placement") String placement);

    @Query("SELECT knotId FROM FullNotation fn WHERE fn.strandId = 0")
    Long getCurrKnotId();

    @Query("SELECT MAX(fn.strandId) FROM FullNotation fn")
    Long getMaxStrandId();
}
