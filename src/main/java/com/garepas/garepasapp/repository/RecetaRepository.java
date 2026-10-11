package com.garepas.garepasapp.repository;

import com.garepas.garepasapp.entity.Receta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    @EntityGraph(attributePaths = {"detalles", "detalles.insumo"})
    List<Receta> findAll();

    @EntityGraph(attributePaths = {"detalles", "detalles.insumo"})
    Optional<Receta> findById(Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Receta> findByNombreContainingIgnoreCase(String nombre);
}
