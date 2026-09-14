package com.knots.backend.repositories;

import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.entities.InvariantsRolf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvariantRolfRepo extends JpaRepository<InvariantsRolf, Long> {
    @Query("SELECT new com.knots.backend.models.dtos.InvariantsDto(i.alexander_polynomial, i.determinant, i.writhe) FROM InvariantsRolf i WHERE i.knotId = :knotId")
    InvariantsDto findByKnotId(@Param("knotId") Long knotId);
}
