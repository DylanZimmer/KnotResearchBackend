package com.knots.backend.repositories;

import com.knots.backend.models.entities.CrossingSpecs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrossingSpecsRepo extends JpaRepository<CrossingSpecs, Long> {
}
