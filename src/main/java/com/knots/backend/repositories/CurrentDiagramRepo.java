package com.knots.backend.repositories;

import com.knots.backend.models.entities.CurrentDiagram;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrentDiagramRepo extends JpaRepository<CurrentDiagram, Long> {
    CurrentDiagram findFirstBy();
}