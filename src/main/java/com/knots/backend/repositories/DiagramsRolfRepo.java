package com.knots.backend.repositories;

import com.knots.backend.models.entities.DiagramsRolf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiagramsRolfRepo extends JpaRepository<DiagramsRolf, Long> {
    @Query("""
            SELECT diagramId
            FROM DiagramsRolf
            WHERE knotId = :kid
    """)
    Long getDiagramIdByKnotId(Long kid);
}
