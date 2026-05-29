package com.knots.backend.repositories;

import com.knots.backend.models.entities.VerticesAndArrows;
import com.knots.backend.models.dtos.Coordinates;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerticesAndArrowsRepo extends JpaRepository<VerticesAndArrows, Long> {
    Long findEndPointByStartPoint(Long startPoint);
    @Query("""
            SELECT new com.knots.backend.models.dtos.Coordinates(v.strandX, v.strandY)
            FROM VerticesAndArrows v
            WHERE v.startPoint = :startPoint
    """)
    Coordinates findStrandCoordinatesFromStartPoint(@Param("startPoint") Long startPoint);
}