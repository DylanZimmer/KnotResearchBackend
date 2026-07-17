package com.knots.backend.repositories;

import com.knots.backend.models.dtos.GeometricLine;
import com.knots.backend.models.dtos.HalfGeometricLine;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.entities.FullNotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    @Query("SELECT GREATEST(MAX(fn.arcIn), MAX(fn.arcOut))  FROM FullNotation fn")
    Long getMaxArc();

    @Query("""
        SELECT new com.knots.backend.models.dtos.HalfGeometricLine(fn.crossingId, fn.placement)
        FROM FullNotation fn
        WHERE (:arcIn = 0 AND fn.arcOut = :maxArc)
        OR (:arcIn = :maxArc AND fn.arcOut = 0)
    """)
    HalfGeometricLine getLineFrom0OrMaxArcIn(@Param("arcIn") Long arcIn, @Param("maxArc") Long maxArc);

    @Query("""
        SELECT new com.knots.backend.models.dtos.HalfGeometricLine(fn.crossingId, fn.placement)
        FROM FullNotation fn
        WHERE (:arcOut = 0 AND fn.arcIn = :maxArc)
        OR (:arcOut = :maxArc AND fn.arcIn = 0)
    """)
    HalfGeometricLine getLineFrom0OrMaxArcOut(@Param("arcOut") Long arcOut, @Param("maxArc") Long maxArc);

    @Query("""
        SELECT new com.knots.backend.models.dtos.HalfGeometricLine(fn.crossingId, fn.placement)
        FROM FullNotation fn
        WHERE ABS(fn.arcOut - :arcIn) = 1
        AND fn.arcIn <> :arcIn
    """)
    HalfGeometricLine getLineFromArcIn(@Param("arcIn") Long arcIn);

    @Query("""
        SELECT new com.knots.backend.models.dtos.HalfGeometricLine(fn.crossingId, fn.placement)
        FROM FullNotation fn
        WHERE ABS(fn.arcIn - :arcOut) = 1
        AND fn.arcOut <> :arcOut
    """)
    HalfGeometricLine getLineFromArcOut(@Param("arcOut") Long arcIn);

    @Query("""
        SELECT new com.knots.backend.models.dtos.LongPair(fn.arcIn, fn.arcOut)
        FROM FullNotation fn
        WHERE fn.crossingId = :cid
        AND fn.placement <> :placement
    """)
    List<LongPair> getArcsOfCrossingPairs(@Param("cid") Long cid, @Param("placement") String placement);

    @Query("""
        SELECT new com.knots.backend.models.dtos.HalfGeometricLine(fn.crossingId, fn.placement)
        FROM FullNotation fn
        WHERE fn.arcIn = :arcIn
    """)
    HalfGeometricLine getLineOfArcIn(@Param("arcIn") Long arcIn);

    @Query("""
        SELECT new com.knots.backend.models.dtos.HalfGeometricLine(fn.crossingId, fn.placement)
        FROM FullNotation fn
        WHERE fn.arcOut = :arcOut
    """)
    HalfGeometricLine getLineOfArcOut(@Param("arcOut") Long arcOut);

    /*
    @Query("""
        SELECT new com.knots.backend.models.dtos.LongPair(
            CASE
                WHEN ABS(hL1.arcIn - hl2.arcOut) = 1
                    THEN hL1.arcIn
                ELSE hl2.arcIn
            END,
            CASE
                WHEN ABS(hl1.arcOut - hl2.arcIn) = 1
                    THEN hl1.arcOut
                ELSE hl2.arcOut
            END )
        FROM FullNotation hl1, FullNotation hl2
        WHERE hl1.crossingId = :cid1
        AND hl1.placement = :placement1
        AND hl2.crossingId = :cid2
        AND hl2.placement = :placement2
    """)
    LongPair getHalfLineAsArcs(@Param("cid1") Long cid1, @Param("placement1") String placement1, @Param("cid2") Long cid2, @Param("placement2") String placement2);
    */

    @Query("""
        UPDATE FullNotation
        SET
        arcIn = CASE
            WHEN arcIn > :startArc THEN arcIn + 4
            ELSE arcIn
        END,
        arcOut = CASE
            WHEN arcOut > :startArc THEN arcOut + 4
            ELSE arcOut
        END
    """)
    void nudgeArcs(@Param("startArc") Long startArc);



}