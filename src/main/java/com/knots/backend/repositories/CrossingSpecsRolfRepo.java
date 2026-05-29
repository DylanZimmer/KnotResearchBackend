package com.knots.backend.repositories;

import com.knots.backend.models.entities.CrossingSpecsRolf;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CrossingSpecsRolfRepo extends JpaRepository<CrossingSpecsRolf, Long> {
    List<CrossingSpecsRolf> findAllByDiagramIdOrderByCrossingIdAsc(Long diagramId);
}
