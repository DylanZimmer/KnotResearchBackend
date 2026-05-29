package com.knots.backend.repositories;

import com.knots.backend.models.entities.VerticesAndArrowsRolf;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VerticesAndArrowsRolfRepo extends JpaRepository<VerticesAndArrowsRolf, Long> {
    List<VerticesAndArrowsRolf> findAllByDiagramIdOrderByStartPointAsc(Long diagramId);
}
