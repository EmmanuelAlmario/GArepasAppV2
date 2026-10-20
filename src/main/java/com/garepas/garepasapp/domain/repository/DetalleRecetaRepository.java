package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.DetalleReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleRecetaRepository extends JpaRepository<DetalleReceta, Long> {

    List<DetalleReceta> findByRecetaId(Long recetaId);
}
