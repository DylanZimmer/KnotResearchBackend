package com.knots.backend.repositories;

import com.knots.backend.models.entities.VerticesAndArrows;
import com.knots.backend.models.dtos.LongPair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerticesAndArrowsRepo extends JpaRepository<VerticesAndArrows, Long> {
    @Query("""
            SELECT v.endPoint
            FROM VerticesAndArrows v
            WHERE v.startPoint = :startPoint
    """)
    Long findEndPointByStartPoint(@Param("startPoint") Long startPoint);
    @Query("""
            SELECT new com.knots.backend.models.dtos.LongPair(v.strandX, v.strandY)
            FROM VerticesAndArrows v
            WHERE v.startPoint = :startPoint
    """)
    LongPair findStrandCoordinatesFromStartPoint(@Param("startPoint") Long startPoint);
}