package com.garepas.garepasapp.repository;

import com.garepas.garepasapp.entity.Produccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProduccionRepository extends JpaRepository<Produccion, Long> {

    @EntityGraph(attributePaths = {"producto", "detalles", "detalles.insumo"})
    List<Produccion> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = {"producto", "detalles", "detalles.insumo"})
    Page<Produccion> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"producto", "detalles", "detalles.insumo"})
    Optional<Produccion> findById(Long id);

    List<Produccion> findByProductoId(Long productoId);

    List<Produccion> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
}
