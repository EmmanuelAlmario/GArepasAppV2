package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.DetalleProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleProduccionRepository extends JpaRepository<DetalleProduccion, Long> {

    List<DetalleProduccion> findByProduccionId(Long produccionId);

    List<DetalleProduccion> findBySuficienteFalse();
}
