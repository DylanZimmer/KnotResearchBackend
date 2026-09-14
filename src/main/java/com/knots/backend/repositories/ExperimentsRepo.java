package com.knots.backend.repositories;

import com.knots.backend.models.entities.Experiments;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ExperimentsRepo extends JpaRepository<Experiments, Long> {
}
