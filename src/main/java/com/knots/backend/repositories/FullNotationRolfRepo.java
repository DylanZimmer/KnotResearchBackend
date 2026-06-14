package com.knots.backend.repositories;

import com.knots.backend.models.entities.FullNotationRolf;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FullNotationRolfRepo extends JpaRepository<FullNotationRolf, Long> {
    List<FullNotationRolf> findByKnotId(Long knotId);
}