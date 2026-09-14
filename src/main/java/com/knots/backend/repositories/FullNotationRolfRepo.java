package com.knots.backend.repositories;

import com.knots.backend.models.entities.FullNotationRolf;

import com.knots.backend.models.keys.FullNotationKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FullNotationRolfRepo extends JpaRepository<FullNotationRolf, FullNotationKey> {
    List<FullNotationRolf> findById_KnotId(Long knotId);
}