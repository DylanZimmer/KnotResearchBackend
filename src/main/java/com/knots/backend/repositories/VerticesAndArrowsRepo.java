package com.knots.backend.repositories;

import com.knots.backend.models.entities.VerticesAndArrows;
import com.knots.backend.models.dtos.LongPair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface VerticesAndArrowsRepo extends JpaRepository<VerticesAndArrows, Long> {

    @Query("""
    SELECT new com.knots.backend.models.dtos.LongPair(va.strandX, va.strandY)
    FROM VerticesAndArrows va
    WHERE va.point = (
        CASE
            WHEN :pt = (SELECT MAX(v.point) FROM VerticesAndArrows v)
            THEN 0
            ELSE :pt + 1
        END
    )
    """)
    LongPair getNextCoords(@Param("pt") Long pt);
}
