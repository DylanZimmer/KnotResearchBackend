package com.knots.backend.repositories;

import com.knots.backend.models.dtos.TwoXYPairs;
import com.knots.backend.models.entities.VerticesAndArrows;
import com.knots.backend.models.dtos.LongPair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VerticesAndArrowsRepo extends JpaRepository<VerticesAndArrows, Long> {
    /*
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
     */

    /*
    @Query("""
        SELECT new com.knots.backend.models.dtos.TwoXYPairs(v1.strandX, v1.strandY, v2.strandX, v2.strandY)
        FROM VerticesAndArrows v1, VerticesAndArrows v2
        WHERE v1.point = :pt1
        AND v2.point = :pt2
    """)
    TwoXYPairs getSegFromCrossingLine(@Param("pt1") Long pt1, @Param("pt2") Long pt2);
    */
    @Query("""
        SELECT new com.knots.backend.models.dtos.LongPair(va.strandX, va.strandY)
        FROM VerticesAndArrows va
        WHERE va.point = :pt
    """)
    LongPair getCoordFromPt(@Param("pt") Long pt);
}